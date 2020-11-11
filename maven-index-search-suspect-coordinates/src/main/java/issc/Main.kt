package issc

import org.apache.lucene.index.MultiFields
import org.apache.maven.index.Indexer
import org.apache.maven.index.context.IndexCreator
import org.apache.maven.index.context.IndexUtils
import org.apache.maven.index.updater.IndexUpdateRequest
import org.apache.maven.index.updater.IndexUpdater
import org.apache.maven.index.updater.WagonHelper
import org.apache.maven.wagon.Wagon
import org.apache.maven.wagon.events.TransferEvent
import org.apache.maven.wagon.observers.AbstractTransferListener
import org.codehaus.plexus.DefaultContainerConfiguration
import org.codehaus.plexus.DefaultPlexusContainer
import org.codehaus.plexus.PlexusConstants
import org.codehaus.plexus.PlexusContainer
import org.codehaus.plexus.component.repository.exception.ComponentLookupException
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

object Main {

    private val plexusContainer: PlexusContainer
    private val indexer: Indexer
    private val indexUpdater: IndexUpdater
    private val httpWagon: Wagon

    @JvmStatic
    fun main(args: Array<String>) {
        perform()
    }

    init {
        val config = DefaultContainerConfiguration()
        config.classPathScanning = PlexusConstants.SCANNING_INDEX
        this.plexusContainer = DefaultPlexusContainer(config)
        this.indexer = plexusContainer.lookup(Indexer::class.java)
        this.indexUpdater = plexusContainer.lookup(IndexUpdater::class.java)
        this.httpWagon = plexusContainer.lookup(Wagon::class.java, "http")
    }

    @Throws(IOException::class, ComponentLookupException::class)
    private fun perform() {
        // Files where local cache is and Lucene Index should be located
        val centralLocalCache = File("target/central-cache")
        val centralIndexDir = File("target/central-index")

        // Creators we want to use (search for fields it defines)
        val indexers = ArrayList<IndexCreator>()
        indexers.add(plexusContainer.lookup(IndexCreator::class.java, "min"))
        indexers.add(plexusContainer.lookup(IndexCreator::class.java, "jarContent"))
        indexers.add(plexusContainer.lookup(IndexCreator::class.java, "maven-plugin"))

        // Create context for central repository index
        val centralContext = indexer.createIndexingContext("central-context", "central", centralLocalCache, centralIndexDir,
            "https://repo.maven.apache.org/maven2/", null, true, true, indexers)

        println("Updating Index...")
        println("This might take a while on first run, so please be patient!")
        val listener = object : AbstractTransferListener() {
            override fun transferStarted(transferEvent: TransferEvent) {
                print("  Downloading " + transferEvent.resource.name)
            }

            override fun transferProgress(transferEvent: TransferEvent?, buffer: ByteArray?, length: Int) {}

            override fun transferCompleted(transferEvent: TransferEvent?) {
                println(" - Done")
            }
        }
        val resourceFetcher = WagonHelper.WagonFetcher(httpWagon, listener, null, null)
        val centralContextCurrentTimestamp = centralContext.timestamp
        val updateRequest = IndexUpdateRequest(centralContext, resourceFetcher)
        val updateResult = indexUpdater.fetchAndUpdateIndex(updateRequest)
        when {
            updateResult.isFullUpdate -> println("Full update happened!")
            updateResult.timestamp == centralContextCurrentTimestamp -> println("No update needed, index is up to date!")
            else -> println("Incremental update happened, change covered $centralContextCurrentTimestamp - ${updateResult.timestamp} period.")
        }

        println()
        println("Using index")
        println()

        // list all the GAVs and find suspect coordinates
        val suspiciousCoordinatesFile = File("suspiciousCoordinates.txt")
        if (suspiciousCoordinatesFile.exists() && !suspiciousCoordinatesFile.delete()) {
            throw RuntimeException("cannot delete " + suspiciousCoordinatesFile.absolutePath)
        }
        if (!suspiciousCoordinatesFile.createNewFile()) {
            throw RuntimeException("cannot create " + suspiciousCoordinatesFile.absolutePath)
        }
        val suspiciousCoordinatesFilteredFile = File("suspiciousCoordinates-filtered.txt")
        if (suspiciousCoordinatesFilteredFile.exists() && !suspiciousCoordinatesFilteredFile.delete()) {
            throw RuntimeException("cannot delete " + suspiciousCoordinatesFilteredFile.absolutePath)
        }
        if (!suspiciousCoordinatesFilteredFile.createNewFile()) {
            throw RuntimeException("cannot create " + suspiciousCoordinatesFilteredFile.absolutePath)
        }
        val searcher = centralContext.acquireIndexSearcher()
        val coordinates = HashMap<String, String>() // key: artifactId, value: groupId
        val suspectCoordinates = HashSet<String>()
        val commonArtifactIds = HashSet(listOf(
            "analytic", "analytics",
            "app",
            "cli",
            "sdk",
            "test"))
        val commonGroupiIds = HashSet(listOf(
            "com.github",
            "net.sourceforge"))

        try {
            val suspectFilteredCoordinates = HashSet<String>()
            val ir = searcher.indexReader
            val liveDocs = MultiFields.getLiveDocs(ir)
            for (i in 0 until ir.maxDoc()) {
                if (i % 100000 == 0) {
                    println("$i/${ir.maxDoc()} - ${i * 100 / ir.maxDoc()}%")
                }
                if (liveDocs == null || liveDocs.get(i)) {
                    val doc = ir.document(i)
                    val ai = IndexUtils.constructArtifactInfo(doc, centralContext)

                    if (ai != null) {
                        val artifactId = ai.artifactId
                        val groupId = ai.groupId

                        if (coordinates.containsKey(artifactId)) {
                            val existingCoor = "${coordinates[artifactId]}:$artifactId"
                            val duplicatedCoor = "$groupId:$artifactId"
                            if (existingCoor != duplicatedCoor && // same coordinate but different classifier (null, sources, javadoc)
                                !suspectCoordinates.contains("|$duplicatedCoor|$existingCoor|")) { // ignore |A|B| if |B|A| is registered already
                                val coordinateCouple = "|$existingCoor|$duplicatedCoor|"

                                var skip = false
                                for (c in commonGroupiIds) {
                                    if (coordinateCouple.contains(c)) {
                                        skip = true
                                    }
                                }
                                for (c in commonArtifactIds) {
                                    if (coordinateCouple.contains(":$c")) {
                                        skip = true
                                    }
                                }
                                if (!skip) {
                                    suspectCoordinates.add(coordinateCouple)
                                    if (groupId.contains(coordinates[artifactId]!!) || coordinates[artifactId]!!.contains(groupId)) {
                                        suspectFilteredCoordinates.add("|$existingCoor|$duplicatedCoor|")
                                    }
                                }
                            }
                        }
                        coordinates[artifactId] = groupId
                    }
                }
            }

            val orderedSuspectCoordinates = ArrayList(suspectCoordinates)
            val orderedSuspectFilteredCoordinates = ArrayList(suspectFilteredCoordinates)
            orderedSuspectCoordinates.sort()
            orderedSuspectFilteredCoordinates.sort()
            BufferedWriter(FileWriter(suspiciousCoordinatesFile)).use { bwSuspects ->
                orderedSuspectCoordinates.forEach { s ->
                    bwSuspects.write(s)
                    bwSuspects.newLine()
                }
            }
            BufferedWriter(FileWriter(suspiciousCoordinatesFilteredFile)).use { bwProbableSuspects ->
                orderedSuspectFilteredCoordinates.forEach { s ->
                    bwProbableSuspects.write(s)
                    bwProbableSuspects.newLine()
                }
            }
        } finally {
            centralContext.releaseIndexSearcher(searcher)
        }
    }
}

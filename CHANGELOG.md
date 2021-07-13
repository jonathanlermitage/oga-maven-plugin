## Old GroupIds Alerter - Change Log

### 1.5.4 (WIP)
* fix #26 hide success message if found errors but `failOnError` is set to false.

### 1.5.3 (2021/07/08)
* configure plugin with properties.

(I faced some problems with my GPG setup, so 1.5.0, 1.5.1 and 1.5.2 were not correctly published, but there's no difference with 1.5.3)

### 1.4.0 (2021/05/02)
* ignore-list support.

### 1.3.0 (2021/03/27)
* show the number of definitions loaded.
* show the definitions file update date.
* add some context in definitions. Example: indicates if suggested change reflects Java EE migration to Jakarta EE.
* add an option to not fail the build.

### 1.2.0 (2020/11/15)
* maven-index-search-suspect-coordinates: update Central repository URL to use HTTPS. Thx [froque](https://github.com/froque).
* make the plugin compatible with project with multiples modules. Thx [froque](https://github.com/froque).

### 1.1.0 (2020/04/23)
* fix #2 make the `og-definitions.json` configurable.

### 1.0.0 (2019/07/06)
* the main goal `check` works.

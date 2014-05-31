@echo off

set applicationServerFullName=
set applicationServerName=
set applicationServerTitle=
set applicationServerVersion=
set assembleModulesIntoJars=
set assertNotNull=
set autoscrollFromSource=
set autoscrollToSource=
set compileInBackground=
set deploymentContextPath=
set gaeHome=
set hideEmptyPackages=
set jdkLevel=
set jdkName=
set openInBrowser=
set openInBrowserUrl=
set optimizeImportsBeforeCommit=
set performCodeAnalysisBeforeCommit=
set reformatCodeBeforeCommit=
set selectedWarArtifactId=
set sortByType=
set vmParameters=
set wildcardResourcePatterns=

rem -- Check Command Line --

if not "%1" == "" goto ok
echo Usage: idea goal
echo
echo Goals:
echo     idea     Create workspace files
echo     clean    Delete workspace files
echo     help     Show help
echo     list     Show dependencies
exit 0
:ok

rem -- Concatenate Parameters --
set command = ""

if "%applicationServerFullName%" == "" goto applicationServerFullName
set command=%command% -DapplicationServerFullName="%applicationServerFullName%"
:applicationServerFullName

if "%applicationServerName%" == "" goto applicationServerName
set command=%command% -DapplicationServerName="%applicationServerName%"
:applicationServerName

if "%applicationServerTitle%" == "" goto applicationServerTitle
set command=%command% -DapplicationServerTitle="%applicationServerTitle%"
:applicationServerTitle

if "%applicationServerVersion%" == "" goto applicationServerVersion
set command=%command% -DapplicationServerVersion="%applicationServerVersion%"
:applicationServerVersion

if "%assembleModulesIntoJars%" == "" goto assembleModulesIntoJars
set command=%command% -DassembleModulesIntoJars="%assembleModulesIntoJars%"
:assembleModulesIntoJars

if "%assertNotNull%" == "" goto assertNotNull
set command=%command% -DassertNotNull="%assertNotNull%"
:assertNotNull

if "%autoscrollFromSource%" == "" goto autoscrollFromSource
set command=%command% -DautoscrollFromSource="%autoscrollFromSource%"
:autoscrollFromSource

if "%autoscrollToSource%" == "" goto autoscrollToSource
set command=%command% -DautoscrollToSource="%autoscrollToSource%"
:autoscrollToSource

if "%compileInBackground%" == "" goto compileInBackground
set command=%command% -DcompileInBackground="%compileInBackground%"
:compileInBackground

if "%deploymentContextPath%" == "" goto deploymentContextPath
set command=%command% -DdeploymentContextPath="%deploymentContextPath%"
:deploymentContextPath

if "%gaeHome%" == "" goto gaeHome
set command=%command% -DgaeHome="%gaeHome%"
:gaeHome

if "%hideEmptyPackages%" == "" goto hideEmptyPackages
set command=%command% -DhideEmptyPackages="%hideEmptyPackages%"
:hideEmptyPackages

if "%jdkLevel%" == "" goto jdkLevel
set command=%command% -DjdkLevel="%jdkLevel%"
:jdkLevel

if "%jdkName%" == "" goto jdkName
set command=%command% -DjdkName="%jdkName%"
:jdkName

if "%openInBrowser%" == "" goto openInBrowser
set command=%command% -DopenInBrowser="%openInBrowser%"
:openInBrowser

if "%openInBrowserUrl%" == "" goto openInBrowserUrl
set command=%command% -DopenInBrowserUrl="%openInBrowserUrl%"
:openInBrowserUrl

if "%optimizeImportsBeforeCommit%" == "" goto optimizeImportsBeforeCommit
set command=%command% -DoptimizeImportsBeforeCommit="%optimizeImportsBeforeCommit%"
:optimizeImportsBeforeCommit

if "%performCodeAnalysisBeforeCommit%" == "" goto performCodeAnalysisBeforeCommit
set command=%command% -DperformCodeAnalysisBeforeCommit="%performCodeAnalysisBeforeCommit%"
:performCodeAnalysisBeforeCommit

if "%reformatCodeBeforeCommit%" == "" goto reformatCodeBeforeCommit
set command=%command% -DreformatCodeBeforeCommit="%reformatCodeBeforeCommit%"
:reformatCodeBeforeCommit

if "%selectedWarArtifactId%" == "" goto selectedWarArtifactId
set command=%command% -DselectedWarArtifactId="%selectedWarArtifactId%"
:selectedWarArtifactId

if "%sortByType%" == "" goto sortByType
set command=%command% -DsortByType="%sortByType%"
:sortByType

if "%vmParameters%" == "" goto vmParameters
set command=%command% -DvmParameters="%vmParameters%"
:vmParameters

if "%wildcardResourcePatterns%" == "" goto wildcardResourcePatterns
set command=%command% -DwildcardResourcePatterns="%wildcardResourcePatterns%"
:wildcardResourcePatterns

@echo on
mvn %command% com.github.zhve:idea-maven-plugin:3.0b1:%1

### launcher

simple minecraft launcher.

configure:

`baseURL`, `baseSubdir` in `doomstal.launcher.Launcher`

`doomstal.launcher.Strings`

compile:

`javac -source 7 -target 7 doomstal/launcher/*.java`

run:

`java doomstal.launcher.Launcher`

make jar:

`jar cmf manifest.mf launcher.jar doomstal/launcher/*.class icon.png`

command line arguments:

`noupdate` - skip update check.

`local` - ignore installation directory and use current instead. reads java arguments from `manifest.txt`.

both disable authentication.

### license

none :)

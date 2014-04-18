# JCloudApp

[![works with regenwolken](http://mycrobase.de/wtf/works_with_regenwolken.png)](https://github.com/posativ/regenwolken)

JCloudApp is inspired by [GloudApp](https://github.com/cmur2/gloudapp) and provides a similar feature set including taking screenshots and uploading files to [CloudApp](http://getcloudapp.com/) via a status/tray icon. It runs on all major platforms like Windows, Mac OS X and Linux.

## Features

* Take full screenshot and upload it
* Upload content of clipboard:
    * text
    * files (e.g. copied from your file explorer)
    * images (works well with "Snipping Tool" under Windows)
* Upload file by file chooser dialog
* Save your login and share it nicely with [GloudApp](https://github.com/cmur2/gloudapp) and [cloudapp-cli](https://github.com/cmur2/cloudapp-cli)

The resulting URL will always appear in your clipboard after finished upload.

## Install

Make sure you have Java (JRE) installed.
[Download a recent JCloudApp](https://github.com/cmur2/jcloudapp/releases).
Extract the archive.
Double-click the `jcloudapp.jar` in the new *jcloudapp* directory. If this doesn't work, use one of the launch scripts for your platform.

## Special guest: Windows users

The screenshot upload workflow is especially suited for Windows users since they have this little nifty tool called *Snipping Tool*:

* Press the Windows key/open start menu and type `snip`, the first match should be the Snipping Tool program
* Hit enter
* You are now in the position to select a portion of your screen to upload, simply click and drag a rectangle
* On release the shown portion is already in your clipboard so now simply double-click JCloudApp's icon in the task bar to upload
* Wait upon the upload-finished notification
* Now you have the URL to your image in your clipboard, paste it in some chat/on twitter/somewhere and have fun

## Compile

Just clone JCloudApp from [this](https://github.com/cmur2/jcloudapp) repository and run in order to retrieve dependencies and compile:

    ant resolve build

(You'll need [Apache Ivy](https://ant.apache.org/ivy/) that uses maven repositories to lookup and download dependencies.)

## Libraries

JCloudApp builds on top of some good libraries:

- [slightly modified CloudAppJava](https://github.com/simong/CloudAppJava)
- [JSON for Java](https://github.com/douglascrockford/JSON-java)
- [HttpComponents](https://hc.apache.org/)

## Credits

To [Jan Graichen](https://github.com/jgraichen) for his nice art work! (borrowed from [GloudApp](https://github.com/cmur2/gloudapp))

## License

JCloudApp is licensed under the Apache License, Version 2.0. See LICENSE for more information.

JCloudApp
=========

JCloudApp is inspired by [GloudApp](https://github.com/cmur2/gloudapp) and
provides a similar feature set including taking screenshots and uploading files
to [CloudApp](http://getcloudapp.com/) via a status/tray icon. It runs on
all major platforms like Windows, Mac OS X and Linux.

**[Works with regenwolken](https://github.com/posativ/regenwolken)**

Features
--------

* Take full screenshot and upload it
* Upload content of clipboard:
    * text
    * files (e.g. copied from your file explorer)
    * images (works well with "Snipping Tool" under Windows)
* Upload file by file chooser dialog
* Save your login and share it nicely with [GloudApp](https://github.com/cmur2/gloudapp) and [cloudapp-cli](https://github.com/cmur2/cloudapp-cli)

The resulting URL will always appear in your clipboard after finished upload.

Install
-------

[Download recent JCloudApp](https://github.com/cmur2/jcloudapp/downloads).
Extract the archive. Double-click the `jcloudapp.jar` in the new *jcloudapp*
directory. If this doesn't work, use one of the launch scripts for your platform.

Compile
-------

Just clone JCloudApp from [this](https://github.com/cmur2/jcloudapp)
repository and run in order to retrieve dependencies and compile:

    ant resolve build

(You'll need [Apache Ivy](https://ant.apache.org/ivy/) that uses maven repositories
to lookup and download dependencies.)

Libraries
---------

JCloudApp builds on top of some good libraries:

- [slightly modified CloudAppJava](https://github.com/simong/CloudAppJava)
- [JSON for Java](https://github.com/douglascrockford/JSON-java)
- [HttpComponents](https://hc.apache.org/)

Thanks
------

To [Jan Graichen](https://github.com/jgraichen) for his nice art work!
(borrowed from [GloudApp](https://github.com/cmur2/gloudapp))

License
-------

JCloudApp is licensed under the Apache License, Version 2.0. See LICENSE for more information.

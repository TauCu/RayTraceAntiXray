# RayTraceAntiXray

This project is a fork of Stonar96's RayTraceAntiXray with the following changes:
* Removed ProtocolLib dependency - the plugin hooks directly into the netty pipeline now.
* Added a reload command.
* Improved Folia support.
* A number of performance improvements.

Paper plugin for server-side async multithreaded ray tracing to hide ores that are exposed to air using Paper Anti-Xray engine-mode 1.

Paper Anti-Xray can't hide ores that are exposed to air in caves for example (see picture below). This plugin is an add-on for Paper Anti-Xray to hide those ores too, using ray tracing to calculate whether or not those ores are visible to players. This plugin can also fully hide block entities such as chests since Minecraft 1.20.6.

![RayTraceAntiXray](https://user-images.githubusercontent.com/18699205/185815590-4b2efce6-5a26-4579-b079-e9958a454fd0.gif)
## How to install
* Download and install [Paper](https://papermc.io/downloads/paper) or Folia 26.3.
* Enable [Paper Anti-Xray](https://docs.papermc.io/paper/anti-xray/) using `engine-mode: 1`.
* Download and install [RayTraceAntiXray](https://builtbybit.com/resources/raytraceantixray.24914/). (For older Minecraft versions, browse the update history.)
* Configure RayTraceAntiXray by editing the file plugins/RayTraceAntiXray/[config.yml](RayTraceAntiXray/src/main/resources/config.yml).
* See also: [Recommended settings](https://gist.github.com/stonar96/69ca0311392188b7ac2ece226286147f).

## Known issues
* Depending on the number of players and config settings, this plugin can be resource intensive. I only recommend using it if you have "unused" CPU threads available on your server in order to minimize the impact on the main thread.
* The culling algorithm is intentionally not 100% accurate for performance and functional reasons. When in doubt, it is assumed that a block is visible. Thus hidden blocks tend to be revealed rather earlier than late, provided that the server isn't overloaded and doesn't lag. Usually, however, this cannot be abused.
* Do not use plugin managers or `/reload` to load, unload or reload this plugin. It won't work properly and will cause issues.

## FAQ
* The plugin isn't hiding any blocks.
* The plugin is ignoring `ray-trace-blocks` entries in the config.
* Ores aren't protected above a certain Y level.
  * This plugin builds on top of Paper's Anti-Xray. Make sure it is configured correctly and enabled.
* It doesn't work properly for players using third-person.
  * Enable `world-settings.<world or default>.ray-trace-third-person` in the plugin config.
  * Please note that this doubles the amount of raytracing done.
* Can I download a pre-compiled binary?
  * The author of the plugin is selling it and doesn't wish others to distribute compiled jars.
  * Instead, follow the [Building](#Building) instructions below.

## Building
### Prerequisites:
* Basic knowledge of your operating system's command prompt/console/terminal
* [Java Development Kit 25 (JDK 25)](https://adoptium.net/temurin/releases)
* A few spare gigabytes of RAM.

### Instructions:
* Download this project: 
  * Click the `Code` tab on the top left of the header bar
  * Click the green `Code` dropdown button and click `Download ZIP`
  * Extract the Zip file to a folder somewhere  on your Computer.
* Open a command prompt/terminal inside the folder where you extracted the source code.
* Execute `java --version` and ensure JDK 25 is correctly installed.
* Compile the plugin with the following command: 
  * Windows: `gradlew.bat clean build`
  * Linux/MacOS: `./gradlew clean build`
* Wait for the build process to complete (usually takes a few minutes on the first run)
* The compiled jar will be saved to `./build/libs/raytraceantixray-*.jar`

## Demo
![RayTraceAntiXray](https://user-images.githubusercontent.com/18699205/112784731-aed75e00-9052-11eb-92d6-b0dd4af79290.gif)

## License
The [LICENSE](LICENSE) file applies to the **source code** of this project. Please don't (re)distribute **compiled binary versions** of this project or derivative works that are directly usable as intended by this project. Shading or using this project as a library for other purposes is permitted.

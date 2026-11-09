# Unplugged Paper

Unplugged paper is a Minecraft Paper plugin to allow you to "unplug" from the server. You get kicked, but a 
clone joins, as if you were still there. This allows you to AFK on the server while you turn your client off.
This is an unofficial paper version of [Unplugged AFK](https://modrinth.com/mod/unplugged-afk), but there is no 1:1 feature parity, YET.

## Features

Currently, the plugin has the following features:
- `/unplug <duration in minutes> <reason>`
  - Unplugs you, with the specified duration and reason if specified
- When you unplug, you get kicked from the server
- A fake player (powered by [FakePlayerPlus](https://github.com/xiplugin/FakePlayerPlus/)) joins
  - It has your name, UUID and other player data
  - It behaves as if you were connected to the server, but AFK and emulated by the server
  - If it gets killed, it automatically leaves
- A lot of Unplugged-afk's configuration options in `plugins/UnpluggedPaper/config.yml`
  - Not all are implemented yet, ones that aren't are prefixed in the comment by `[DOES NOTHING ATM]`

## Differences with Unplugged AFK

> I will be referring to Unplugged AFK as "UA", and Unplugged Paper as "UP"

This project aims to have full feature compatibility with UA, as in, what you can do with UA, can be done with this plugin.
For example, if you have a datapack that uses UA commands, it *SHOULD* work with UP (as long as they use `/unplug` and not `/afk`).
But due to fundamental platform differences, the behaviour may not be 100% the same.
The biggest difference you may notice is the config. For UP, we use YAML, instead of JSON.
Some config options are missing from UP, mainly the permission levels options, because on Paper we use permissions for that.

Full feature difference list:
- YAML instead of JSON for config
- Config options removed:
  - `unplugCommandPermissions`
  - `unpluggedAdminCommandPermissions`
  - `afkCommandPermissions`
  - `enableUnplugCommand`
  - `enableAfkCommand`
- No `/afk` command, I recommend you use another plugin that allows you to create command aliases if you want this command back.
- All times are relative to IRL time, not minecraft ticks (it seems unplugged-afk kinda relies on ticks, not quite sure)
- Not all configs implemented yet
- `/unplugged-admin` command only half implemented
  - `reload` implemented, shouldn't really be different
  - `list` implemented, but format is slightly different
  - `purge` implemented, but doesn't add entries for online players back yet
  - `info` implemented, but format just as different as list
  - `spawn`, `kick`, `save` and `set` are not implemented yet

## Install

To install this, you first need to set up a [Paper](https://papermc.io/software/paper/) server. Follow those instructions first.
When you have done that, you need to obtain 2 jars, the Unplugged Paper one, and a FakePlayerPlus one.
To obtain these, there are 2 options: build yourself, or download from the github releases.

### GitHub Releases

Download the latest version of Unplugged Paper from the [releases page](https://github.com/electricsteve/UnpluggedPaper/releases).
This page should also link to the correct version of FakePlayerPlus to use, which is currently the latest from my fork.
The releases for my fork of FakePlayerPlus are [here](https://github.com/electricsteve/FakePlayerPlus/releases).

### Local Build

If you'd like to build the plugin yourself, you will also need to build FakePlayerPlus yourself.
Step-by-step tutorial: (replace `./gradlew` with `gradlew.bat` on windows)
1. `git clone https://github.com/electricsteve/FakePlayerPlus/`
2. `cd FakePlayerPlus` to go to the cloned project
3. `APP_VERSION=2.2.51 ./gradlew build publishToMavenLocal` to build and publish the artifact to the local maven repository
4. `cd ..` to go back
5. `git clone https://github.com/electricsteve/UnpluggedPaper`
6. `cd UnpluggedPaper`
7. `./gradlew build`

Then you take the jars from `FakePlayerPlus/build/libs/FakePlayerPlus-2.2.51.jar` and `UnpluggedPaper/build/libs/UnpluggedPaper-0.1.2.jar`
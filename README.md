[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/yegor256/npm-files)](http://www.rultor.com/p/yegor256/npm-files)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Build Status](https://img.shields.io/travis/yegor256/npm-files/master.svg)](https://travis-ci.org/yegor256/npm-files)
[![Javadoc](http://www.javadoc.io/badge/com.yegor256/npm-files.svg)](http://www.javadoc.io/doc/com.yegor256/npm-files)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/yegor256/npm-files/blob/master/LICENSE.txt)
[![Hits-of-Code](https://hitsofcode.com/github/yegor256/npm-files)](https://hitsofcode.com/view/github/yegor256/npm-files)
[![Maven Central](https://img.shields.io/maven-central/v/com.yegor256/npm-files.svg)](https://maven-badges.herokuapp.com/maven-central/com.yegor256/npm-files)
[![PDD status](http://www.0pdd.com/svg?name=yegor256/npm-files)](http://www.0pdd.com/p?name=yegor256/npm-files)

Similar solutions:

   * [Artifactory](https://www.jfrog.com/confluence/display/RTF/npm+Registry)
   * [Package cloud](https://packagecloud.io/docs#node_npm)

References: 

   * [NPM registry internals](https://blog.packagecloud.io/eng/2018/01/24/npm-registry-internals/)
   * [CommonJS Package Registry specification](http://wiki.commonjs.org/wiki/Packages/Registry)
   * [The JavaScript Package Registry](https://docs.npmjs.com/misc/registry)

This is the dependency you need:

```xml
<dependency>
  <groupId>com.yegor256</groupId>
  <artifactId>rpm-files</artifactId>
  <version>[...]</version>
</dependency>
```

TBD...

## How it works?

TBD...

## How to contribute

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```

To avoid build errors use Maven 3.2+.

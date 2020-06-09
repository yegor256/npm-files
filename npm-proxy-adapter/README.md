<img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/>

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/npm-proxy-adapter)](http://www.rultor.com/p/artipie/npm-proxy-adapter)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Build Status](https://img.shields.io/travis/artipie/npm-proxy-adapter/master.svg)](https://travis-ci.org/artipie/npm-proxy-adapter)
[![Javadoc](http://www.javadoc.io/badge/com.artipie/npm-proxy-adapter.svg)](http://www.javadoc.io/doc/com.artipie/npm-proxy-adapter)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/npm-proxy-adapter/blob/master/LICENSE.txt)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/npm-proxy-adapter)](https://hitsofcode.com/view/github/artipie/npm-proxy-adapter)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/npm-proxy-adapter.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/npm-proxy-adapter)
[![PDD status](http://www.0pdd.com/svg?name=yegor256/npm-files)](http://www.0pdd.com/p?name=yegor256/npm-files)

# npm-proxy-adapter

## Usage
This is the dependency you need:

```xml
<dependency>
  <groupId>com.artipie</groupId>
  <artifactId>npm-proxy-adapter</artifactId>
  <version>[...]</version>
</dependency>
```

## How it works?

After initial setup of the proxy repository, it can be used to work with NPM.
NPM registry to install package from can be specified explicitly 
`npm install --registry=https://artipie.com/npm-proxy-registry <package_name>` or setup 
as default with `npm set registry https://artipie.com/npm-proxy-registry` command. 

NPM sends HTTP requests to specified URL and adapter processes them. It proxies 
request to the remote repository previously setup. After getting artifact, it caches 
received data and transform before sending it back to client. The next request will not 
require remote repository call - the cached data will be used.

## Technical details

Detailed explanation about used algorithms and protocols can be found here: 
[implementation.md](implementation.md) 

## How to contribute

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```

To avoid build errors use Maven 3.2+.
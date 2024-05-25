# Janilla eShopOnWeb

This is a porting of [Microsoft eShopOnWeb ASP.NET Core Reference Application](https://github.com/dotnet-architecture/eShopOnWeb).

### What it does and why it is useful

The goal for this sample is to demonstrate some of the principles and patterns for developing modern web applications with Janilla. It is not meant to be an eCommerce reference application, and as such it does not implement many features that would be obvious and/or essential to a real eCommerce application.

### How you can get started

> **_Note:_**  if you are unfamiliar with the terminal, you can set up the project in an IDE (section below).

Make sure you have Java SE Platform (JDK 21) and [Apache Maven](https://maven.apache.org/install.html) installed.

From the project root, run the following command to run the fullstack application:

```shell
mvn compile exec:java -pl full
```

Then open a browser and navigate to <http://localhost:8080/>.

> **_Note:_**  consider checking the Disable Cache checkbox in the Network tab of the Web Developer Tools.

### Set up the project in an IDE

So far the project has been developed with [Eclipse IDE](https://eclipseide.org/):

1. download the [Eclipse Installer](https://www.eclipse.org/downloads/packages/installer)
2. install the package for Enterprise Java and Web Developers with JRE 21
3. launch the IDE and choose Import projects from Git (with smart import)
4. select GitHub as the repository source, then search for `janilla-eshopweb` and complete the wizard
5. select a project (eg: `janilla-eshopweb-full`) and launch Debug as Java Application
6. open a browser and navigate to <http://localhost:8080/>

### Where you can get help

Please visit [www.janilla.com](https://janilla.com/) for more information.

You can use [GitHub Issues](https://github.com/diego-schivo/janilla-eshopweb/issues) to give or receive feedback.

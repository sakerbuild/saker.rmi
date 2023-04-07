# saker.rmi

[![Build status](https://img.shields.io/azure-devops/build/sakerbuild/5bb4fa52-aa48-4924-957e-f1b25aeff54e/13/master)](https://dev.azure.com/sakerbuild/saker.rmi/_build) [![Latest version](https://mirror.nest.saker.build/badges/saker.rmi/version.svg)](https://nest.saker.build/package/saker.rmi "saker.rmi | saker.nest")

Java RMI (Remote Method Invocation) library implemented from the ground up. It is designed for cooperative processes in mind and allows automatic and transparent usage of object regardless if they're remote or local.

The library is also included in the [saker.build system](https://saker.build) under the `saker.build.thirdparty` package.

See the [documentation](https://saker.build/saker.rmi/doc/) for more information.

## Build instructions

The project uses the [saker.build system](https://saker.build) for building. Use the following command to build the project:

```
java -jar path/to/saker.build.jar -bd build compile main/saker.build
```

## License

The source code for the project is licensed under *GNU General Public License v3.0 only*.

Short identifier: [`GPL-3.0-only`](https://spdx.org/licenses/GPL-3.0-only.html).

Official releases of the project (and parts of it) may be licensed under different terms. See the particular releases for more information.

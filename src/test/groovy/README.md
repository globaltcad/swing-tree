# The SwingTree Testsuite #

---

SwingTree relies on [Spock](https://github.com/spockframework/spock) as its primary testing framework.
If you are not familiar with Spock don't worry, beyond this README
[there is also a simple introduction for writing and reading 
Spock specifications](Example_Spec.groovy) in this directory.

The actual test specifications are located in the local
[swingtree](./swingtree) subdirectory.

## When is Something a Unit? ##

The terms **module** or **component** mentioned earlier
is commonly what's considered as a testable unit.
However, this project does not contain
highly independent modules in the traditional
sense, which is why the vast majority of tests
are unit tests. The integration and system tests found
in this suite on the other hand,
distinguish themselves merely by being more complex and compute intensive tests.
But there are barely any of those in this project, expect for a hand full
of component snapshot based tests which check if components
are rendered correctly.

---

## Tests are living / executable documentation ##

This testsuite ought to be viewed as documentation first and foremost!<br>
If you want to add or change specifications and tests then please <br>
keep this in mind and write them as if they are being read as such. <br>

Specifications will automatically be turned into json reports 
located at [docs/spock/reports](../../../docs/spock/reports) <br>
which can then be viewed in human readable form through the
documentation [index.html](../../../docs/index.html) page.

This page is also publicly available via the
[github pages](https://globaltcad.github.io/swing-tree/). <br>



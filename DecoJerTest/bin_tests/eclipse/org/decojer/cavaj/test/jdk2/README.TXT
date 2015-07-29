Differences to previous

* 1.0 had no inner classes, and so could not pass the JCK.
* 1.1 did not have strictfp, and so could not pass the JCK.

* 1.2 introduced strictfp.


* Change: Inner Anonymous / Local classes have no outer info anymore in inner classes attribute
* Bug: inner classes resolving in JRE 1 not useable,
    e.g. System.out.println(Inner2.Inner21.class) not working,
    only public classes and interfaces in other packages could be accessed

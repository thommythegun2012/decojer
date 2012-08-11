Differences JRE 2 to previous

* Change: Inner Anonymous / Local classes have no outer info anymore in inner classes attribute
* Bug: inner classes resolving in JRE 1 not useable,
    e.g. System.out.println(Inner2.Inner21.class) not working,
    only public classes and interfaces in other packages could be accessed
* New: modifier 'strictfp'

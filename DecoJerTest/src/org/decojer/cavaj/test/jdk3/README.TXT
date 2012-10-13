Differences to previous

* 1.3 is the same language as 1.2.


* Bug: parallel inner local classes in methods with same name not possible in JRE 1,
    they overrided each other (see AInner), now $1$AInner, $2$AInner

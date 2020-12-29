test-clj:
	lein test

test-cljs:
	lein doo node test once

test: test-clj test-cljs

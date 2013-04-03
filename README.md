# onelog

Batteries-included logging for Clojure.

Be aware that this is alpha code. It may have bugs, and the API may
change before the final release. Pull requests are welcome!

## Usage

Require `onelog.core`. Call `(start!)`. Then call `(info "whatever")`, `(warn "foo")`, etc. 

If you want to copy your log message to STDOUT in addition to logging it, call `(info+
"whatever")`, etc.

If you want to log to a specific file, call `(start! "/tmp/foo.log")`.

By default, `(start!)` only works once. That is, subsequent calls to
`start!` are ignored. This is so that multiple libraries can be
required that all provide a default logging context for themselves,
but which also do not overwrite your app's overall logging context
with their own when they're loaded. Just set up your preferred
`start!` context before you start loading other libraries that use
OneLog. See the Claw project for an example of how this can work.

You can provide the `force` argument to `start!` if you want to
override a previous call to `start!`.

More docs to come.

## License

Copyright © 2013 Paul Legato.

Distributed under the Eclipse Public License, the same as Clojure.

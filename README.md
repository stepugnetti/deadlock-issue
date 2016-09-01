# http-deadlock

A minimal example to reproduce a deadlock when using http-kit for serving http requests that block, waiting for messages on Websocket channels.

## Usage

    lein run

Changing the time interval between http requests on line 74 of `src/http_deadlock/core.clj` may make the altenarnating requests and answers block. There is a (machine-dependent) threshold below which a deadlock condition occurs inside http-kit: the thread pool is all taken by http requests and no thread is available to parse the websocket messages that would allow the http requests to be responded.

On my machine (Mac mini, 3 GHz Intel Core i7, 16 GB DDR3 at 1600 MHz) the threshold is 390ms.

## Solution

Since http-kit uses the same thread pool both for responing http requests and for parsing Websocket messages, it is not a good idead to block the thread serving an http request on waiting for an answer on the websocket channel. In case this is needed,  one can instantiate _two_ http-kit server: one serving http requests and the other one managing websocket channels.

package dev.bennett.codexmeter;

/** Process-local intent fence; PKCE secrets are never persisted. */
final class OAuthAttempt {
    interface Commit { void run() throws Exception; }
    private long epoch, active;
    synchronized long begin(){active=++epoch;return active;}
    synchronized boolean current(long attempt){return attempt!=0&&active==attempt;}
    synchronized boolean active(){return active!=0;}
    synchronized boolean epoch(long attempt){return attempt!=0&&epoch==attempt;}
    synchronized void cancel(){active=0;epoch++;}
    synchronized boolean cancel(long attempt){if(!current(attempt))return false;cancel();return true;}
    synchronized boolean finish(long attempt){if(!current(attempt))return false;active=0;return true;}
    synchronized boolean commit(long attempt,Commit work)throws Exception{
        if(!current(attempt))return false;
        work.run();active=0;return true;
    }
}

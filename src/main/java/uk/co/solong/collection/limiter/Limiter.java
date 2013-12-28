package uk.co.solong.collection.limiter;

public interface Limiter {
	public void clockIn() throws InterruptedException;
}

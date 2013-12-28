package uk.co.solong.collections.limiter;

public interface Limiter {
	public void clockIn() throws InterruptedException;
}

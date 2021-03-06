package uk.co.harcourtprogramming.mewler;

import java.io.IOException;
import java.util.Random;
import uk.co.harcourtprogramming.logging.LogDecorator;
import uk.co.harcourtprogramming.mewler.servermesasges.IrcPongMessage;

class IrcPingThread extends Thread
{
	private final static LogDecorator LOG = LogDecorator.getLogger("Mewler");

	private final IrcOutgoingThread outThread;
	private final IrcConnection outer;
	private final Random r = new Random();

	private long nonce;
	private boolean kill;

	IrcPingThread(ThreadGroup tg, IrcConnection outer, IrcOutgoingThread outThread)
	{
		super(tg, "InternetRelayCats.Mewler-Ping-Thread");
		this.outer = outer;
		this.outThread = outThread;
	}

	synchronized void onPong(IrcPongMessage pong)
	{
		if (Long.parseLong(pong.getNonce(), 16) == nonce)
		{
			LOG.finer("PONG nonce value {0} received correctly", pong.getNonce());
			nonce = 0;
		}
		else
		{
			LOG.info("Unexpected PONG nonce value{0}", pong.getNonce());
		}
	}

	@Override
	@SuppressWarnings("empty-statement")
	public synchronized void run()
	{
		try
		{
			this.wait(2000); // Warm up time
			while (ping(r.nextLong()));
		}
		catch (InterruptedException ex)
		{
			LOG.fine("{0} closing", getName());
			return;
		}
		LOG.fine("PONG not received");
		if (outer.isAlive())
		{
			outer.onDisconnect();
			outer.dispose();
		}
	}

	private synchronized boolean ping(long n) throws InterruptedException
	{
		nonce = n;
		try
		{
			outThread.send(IrcCommands.createCommandString(IrcCommands.PING, Long.toString(nonce, 16)));
		}
		catch (IOException ex)
		{
			LOG.severe("Unable to send Ping", ex);
			return false;
		}

		this.wait(120000);

		return (nonce == 0x0L);
	}

	synchronized void dispose()
	{
		kill = true;
		this.interrupt();
	}
}

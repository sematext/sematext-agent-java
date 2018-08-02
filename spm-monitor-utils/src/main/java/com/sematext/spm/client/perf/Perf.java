/*
 * Licensed to Sematext Group, Inc
 *
 * See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Sematext Group, Inc licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sematext.spm.client.perf;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

public final class Perf {
  private static final Log LOG = LogFactory.getLog(Perf.class);

  public static class Histo {
    private final AtomicLong min = new AtomicLong(0);
    private final AtomicLong max = new AtomicLong(0);
    private final AtomicLong count = new AtomicLong(0);
    private final AtomicLong total = new AtomicLong(0);

    private final String name;

    public Histo(String name) {
      this.name = name;
    }

    public Timer measure() {
      return new Timer(System.currentTimeMillis(), this);
    }

    public long getMin() {
      return min.get();
    }

    public long getMax() {
      return max.get();
    }

    public long getCount() {
      return count.get();
    }

    public long getTotal() {
      return total.get();
    }
  }

  public static class Timer {
    private final long startTs;
    private final Histo histo;

    Timer(long startTs, Histo histo) {
      this.startTs = startTs;
      this.histo = histo;
    }

    public void end() {
      long duration = System.currentTimeMillis() - startTs;
      histo.count.incrementAndGet();
      histo.total.addAndGet(duration);

      long minC, min;
      do {
        minC = histo.min.get();
        min = Math.min(duration, minC);
      } while (!histo.min.compareAndSet(min, minC));

      long maxC, max;
      do {
        maxC = histo.max.get();
        max = Math.max(duration, maxC);
      } while (!histo.max.compareAndSet(maxC, max));
    }
  }

  private static final CopyOnWriteArrayList<Histo> HISTOGRAMS = new CopyOnWriteArrayList<Histo>();

  public static Histo histo(String name) {
    final Histo histo = new Histo(name);
    HISTOGRAMS.add(histo);
    return histo;
  }

  private static String printHistogram(final Histo histo) {
    final StringBuilder dump = new StringBuilder();
    dump.append("{ histo = ").append(histo.name).append(" ms,");
    dump.append(" min = ").append(histo.getMin()).append(" ms, ");
    dump.append(" max = ").append(histo.getMax()).append(" ms, ");
    dump.append(" total = ").append(histo.getTotal()).append(" ms, ");
    dump.append(" count = ").append(histo.getCount()).append(", ");

    double average = 0d;
    if (histo.getCount() > 0) {
      average = ((double) histo.getTotal()) / histo.getCount();
    }

    dump.append(" average = ").append(String.format("%2.2f", average)).append(" ms }");
    return dump.toString();
  }

  public static void startConsoleReporter(final long intervalMillis) {
    final Thread reporterThread = new Thread("reporter-thread") {
      @Override
      public void run() {
        try {
          while (true) {
            for (final Histo histo : HISTOGRAMS) {
              LOG.info(printHistogram(histo));
            }
            Thread.sleep(intervalMillis);
          }
        } catch (Exception e) {

        }
      }
    };
    reporterThread.setDaemon(true);
    reporterThread.start();
  }

}

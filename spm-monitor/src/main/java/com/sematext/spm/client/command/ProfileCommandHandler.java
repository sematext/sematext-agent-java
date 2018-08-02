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
package com.sematext.spm.client.command;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.agent.profiler.ProfileSnapshotThriftSerializer;
import com.sematext.spm.client.agent.profiler.Profiler;
import com.sematext.spm.client.agent.profiler.cpu.AllThreadsProfileSnapshot;
import com.sematext.spm.client.jmx.JmxServiceContext;
import com.sematext.spm.client.monitor.thrift.TCommand;
import com.sematext.spm.client.monitor.thrift.TCommandResponse;
import com.sematext.spm.client.monitor.thrift.TCommandResponseStatus;
import com.sematext.spm.client.monitor.thrift.TCommandType;
import com.sematext.spm.client.monitor.thrift.TProfileRequest;
import com.sematext.spm.client.monitor.thrift.TProfileResponse;
import com.sematext.spm.client.snap.serializer.TBinaryProto;

public class ProfileCommandHandler implements CommandHandler {
  private static final Log LOG = LogFactory.getLog(ProfileCommandHandler.class);

  private static final long PROFILE_DURATION_LOW_BOUND = TimeUnit.SECONDS.toMillis(20);
  private static final long PROFILE_DURATION_UP_BOUND = TimeUnit.MINUTES.toMillis(30);

  private JmxServiceContext ctx;

  public ProfileCommandHandler(JmxServiceContext ctx) {
    this.ctx = ctx;
  }

  private final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
    @Override
    public Thread newThread(Runnable r) {
      return new Thread(r, "profile-command-handler");
    }
  });

  @Override
  public Cancellable handle(final TCommand command, final ResponseCallback callback) {
    if (TCommandType.PROFILE == command.getType()) {
      LOG.info("Got profile command.");

      TProfileRequest request = new TProfileRequest();
      TBinaryProto.read(request, command.getRequest());

      if (request.getDurationMillis() < PROFILE_DURATION_LOW_BOUND
          || request.getDurationMillis() > PROFILE_DURATION_UP_BOUND) {
        final TCommandResponse response = new TCommandResponse(TCommandResponseStatus.FAILURE, command.getId());
        response.setFailureReason(
            "Duration should be between " + PROFILE_DURATION_LOW_BOUND + " and " + PROFILE_DURATION_UP_BOUND + " ms ("
                + request.getDurationMillis() + ")");

        callback.respond(response);

        LOG.warn("Profile command failed. Specified duration (" + request.getDurationMillis() + ") is out of bounds ["
                     + PROFILE_DURATION_LOW_BOUND + "," + PROFILE_DURATION_UP_BOUND + "].");
        return null;
      }

      LOG.info("Starting profile session: for " + request.getDurationMillis() + "ms with sampling interval " + request
          .getPeriodMillis() + "ms");

      final long startTs = System.currentTimeMillis();

      Profiler profiler = Profiler.builder()
          .duration(request.getDurationMillis(), TimeUnit.MILLISECONDS)
          .sampleInterval(request.getPeriodMillis(), TimeUnit.MILLISECONDS)
          .excludeAgentClasses(!(!request.isExcludeAgentMethods() || Boolean
              .getBoolean("spm.client.profiler.include.agent.classes")))
          .threadMXBean(JMXClient.getThreadMXBean(ctx))
          .gcMXBeans(JMXClient.getGarbageCollectorMXBeans(ctx))
          .build();

      final SimpleCancellable cancellable = new SimpleCancellable();

      profiler.profile(executor, cancellable, new Profiler.Consumer() {
        @Override
        public void consume(final AllThreadsProfileSnapshot snapshot, Throwable e) {
          if (cancellable.isCancelled()) {
            return;
          }

          if (snapshot != null) {
            if (Boolean.getBoolean("spm.client.profiler.chatty")) {
              System.out.println(snapshot.getTree().dump());
            }

            LOG.info("Profile created, took: " + (System.currentTimeMillis() - startTs) + "ms.");

            try {
              TProfileResponse profileResponse = new TProfileResponse(ProfileSnapshotThriftSerializer
                                                                          .toThrift(snapshot));
              TCommandResponse response = new TCommandResponse(TCommandResponseStatus.SUCCESS, command.getId());
              response.setResponse(TBinaryProto.toByteArray(profileResponse));

              callback.respond(response);
              LOG.info("Profile created and sent.");
            } catch (Throwable thr) {
              LOG.error("Error while processing profiling response", thr);
            }
          } else if (e != null) {
            LOG.info("Profile creation failed: ", e);

            final TCommandResponse response = new TCommandResponse(TCommandResponseStatus.FAILURE, command.getId());
            response.setFailureReason("Exception:" + e.getMessage());
            callback.respond(response);
          }
        }
      });

      return cancellable;
    }
    return null;
  }

}

/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ratpack.zipkin.internal;

import com.github.kristofa.brave.ServerClientAndLocalSpanState;
import com.github.kristofa.brave.ServerSpan;
import com.twitter.zipkin.gen.Endpoint;
import com.twitter.zipkin.gen.Span;
import ratpack.exec.Execution;
import ratpack.registry.MutableRegistry;

import java.util.function.Supplier;


public class RatpackServerClientLocalSpanState implements ServerClientAndLocalSpanState  {
  private final Supplier<MutableRegistry> registry;
  private final Endpoint endpoint;

  public RatpackServerClientLocalSpanState(final String serviceName, int ip, int port) {
    this(serviceName, ip, port, Execution::current);
  }

  RatpackServerClientLocalSpanState(final String serviceName, int ip, int port, final Supplier<MutableRegistry> registry) {
    this.registry = registry;
    this.endpoint = Endpoint.create(serviceName, ip, port);
  }

  @Override
  public Endpoint getClientEndpoint() {
    return registry.get()
        .maybeGet(CurrentClientServiceNameValue.class)
        .map(TypedValue::get)
        .map(currentClientServiceName -> Endpoint.create(currentClientServiceName, this.endpoint.ipv4, this.endpoint.port))
        .orElse(this.endpoint);
  }

  @Override
  public void setCurrentClientSpan(final Span span) {
    registry.get().add(new CurrentClientSpanValue(span));
  }

  @Override
  public Span getCurrentClientSpan() {
    return registry.get()
        .maybeGet(CurrentClientSpanValue.class)
        .map(TypedValue::get)
        .orElse(null);
  }

  @Override
  public void setCurrentClientServiceName(final String serviceName) {
    registry.get().add(new CurrentClientServiceNameValue(serviceName));
  }

  @Override
  public Span getCurrentLocalSpan() {
    return registry.get()
        .maybeGet(CurrentLocalSpanValue.class)
        .map(TypedValue::get)
        .orElse(null);
  }

  @Override
  public void setCurrentLocalSpan(final Span span) {
    registry.get().add(new CurrentLocalSpanValue(span));
  }

  @Override
  public ServerSpan getCurrentServerSpan() {
    return registry.get()
        .maybeGet(CurrentServerSpanValue.class)
        .map(TypedValue::get)
        .orElse(ServerSpan.EMPTY);
  }

  @Override
  public Endpoint getServerEndpoint() {
    return this.endpoint;
  }

  @Override
  public void setCurrentServerSpan(final ServerSpan span) {
    registry.get().add(new CurrentServerSpanValue(span));
  }

  @Override
  public Boolean sample() {
    return getCurrentServerSpan().getSample();
  }

  private abstract class TypedValue<T> {
    private T value;

    TypedValue(final T value) {
      this.value = value;
    }

    T get() {
      return value;
    }
  }

  private class CurrentLocalSpanValue extends TypedValue<Span> {
    CurrentLocalSpanValue(final Span value) {
      super(value);
    }
  }

  private class CurrentClientSpanValue extends TypedValue<Span> {
    CurrentClientSpanValue(final Span value) {
      super(value);
    }
  }

  private class CurrentServerSpanValue extends TypedValue<ServerSpan> {
    CurrentServerSpanValue(final ServerSpan value) {
      super(value);
    }
  }

  private class CurrentClientServiceNameValue extends TypedValue<String> {
    CurrentClientServiceNameValue(final String value) {
      super(value);
    }
  }

}

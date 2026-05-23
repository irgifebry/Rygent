package com.rygent.monitor.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class PollingWorker_AssistedFactory_Impl implements PollingWorker_AssistedFactory {
  private final PollingWorker_Factory delegateFactory;

  PollingWorker_AssistedFactory_Impl(PollingWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public PollingWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<PollingWorker_AssistedFactory> create(
      PollingWorker_Factory delegateFactory) {
    return InstanceFactory.create(new PollingWorker_AssistedFactory_Impl(delegateFactory));
  }
}

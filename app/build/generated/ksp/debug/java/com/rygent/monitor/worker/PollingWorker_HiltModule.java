package com.rygent.monitor.worker;

import androidx.hilt.work.WorkerAssistedFactory;
import androidx.work.ListenableWorker;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.codegen.OriginatingElement;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import javax.annotation.processing.Generated;

@Generated("androidx.hilt.AndroidXHiltProcessor")
@Module
@InstallIn(SingletonComponent.class)
@OriginatingElement(
    topLevelClass = PollingWorker.class
)
public interface PollingWorker_HiltModule {
  @Binds
  @IntoMap
  @StringKey("com.rygent.monitor.worker.PollingWorker")
  WorkerAssistedFactory<? extends ListenableWorker> bind(PollingWorker_AssistedFactory factory);
}

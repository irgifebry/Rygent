package com.rygent.monitor.di;

import com.rygent.monitor.data.local.SystemMonitorDatabase;
import com.rygent.monitor.data.local.dao.DeviceDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class DatabaseModule_ProvideDeviceDaoFactory implements Factory<DeviceDao> {
  private final Provider<SystemMonitorDatabase> dbProvider;

  public DatabaseModule_ProvideDeviceDaoFactory(Provider<SystemMonitorDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public DeviceDao get() {
    return provideDeviceDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideDeviceDaoFactory create(
      Provider<SystemMonitorDatabase> dbProvider) {
    return new DatabaseModule_ProvideDeviceDaoFactory(dbProvider);
  }

  public static DeviceDao provideDeviceDao(SystemMonitorDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDeviceDao(db));
  }
}

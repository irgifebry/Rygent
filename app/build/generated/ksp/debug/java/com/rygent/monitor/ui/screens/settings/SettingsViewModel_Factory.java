package com.rygent.monitor.ui.screens.settings;

import android.content.Context;
import com.rygent.monitor.data.local.SettingsManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<SettingsManager> settingsManagerProvider;

  private final Provider<Context> contextProvider;

  public SettingsViewModel_Factory(Provider<SettingsManager> settingsManagerProvider,
      Provider<Context> contextProvider) {
    this.settingsManagerProvider = settingsManagerProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(settingsManagerProvider.get(), contextProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<SettingsManager> settingsManagerProvider,
      Provider<Context> contextProvider) {
    return new SettingsViewModel_Factory(settingsManagerProvider, contextProvider);
  }

  public static SettingsViewModel newInstance(SettingsManager settingsManager, Context context) {
    return new SettingsViewModel(settingsManager, context);
  }
}

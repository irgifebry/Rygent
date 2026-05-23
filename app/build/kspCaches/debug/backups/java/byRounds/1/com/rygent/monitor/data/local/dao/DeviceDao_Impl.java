package com.rygent.monitor.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.rygent.monitor.data.local.Converters;
import com.rygent.monitor.data.local.entity.DeviceEntity;
import com.rygent.monitor.domain.model.DeviceStatus;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DeviceDao_Impl implements DeviceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DeviceEntity> __insertionAdapterOfDeviceEntity;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<DeviceEntity> __deletionAdapterOfDeviceEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateDeviceConfig;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public DeviceDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDeviceEntity = new EntityInsertionAdapter<DeviceEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `devices` (`id`,`name`,`ipAddress`,`port`,`token`,`status`,`cpuUsage`,`ramUsage`,`diskUsage`,`latency`,`uptime`,`lastSeen`,`processesJson`,`hardwareJson`,`systemJson`,`batteryJson`,`networkJson`,`diskJson`,`appsJson`,`gpuJson`,`cpuUsagePerCoreJson`,`cpuFreqPerCoreJson`,`cpuFrequency`,`cpuTemp`,`diskIoJson`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DeviceEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getIpAddress());
        statement.bindLong(4, entity.getPort());
        if (entity.getToken() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getToken());
        }
        final String _tmp = __converters.fromDeviceStatus(entity.getStatus());
        statement.bindString(6, _tmp);
        statement.bindLong(7, entity.getCpuUsage());
        statement.bindLong(8, entity.getRamUsage());
        if (entity.getDiskUsage() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getDiskUsage());
        }
        statement.bindLong(10, entity.getLatency());
        statement.bindString(11, entity.getUptime());
        statement.bindLong(12, entity.getLastSeen());
        if (entity.getProcessesJson() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getProcessesJson());
        }
        if (entity.getHardwareJson() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getHardwareJson());
        }
        if (entity.getSystemJson() == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, entity.getSystemJson());
        }
        if (entity.getBatteryJson() == null) {
          statement.bindNull(16);
        } else {
          statement.bindString(16, entity.getBatteryJson());
        }
        if (entity.getNetworkJson() == null) {
          statement.bindNull(17);
        } else {
          statement.bindString(17, entity.getNetworkJson());
        }
        if (entity.getDiskJson() == null) {
          statement.bindNull(18);
        } else {
          statement.bindString(18, entity.getDiskJson());
        }
        if (entity.getAppsJson() == null) {
          statement.bindNull(19);
        } else {
          statement.bindString(19, entity.getAppsJson());
        }
        if (entity.getGpuJson() == null) {
          statement.bindNull(20);
        } else {
          statement.bindString(20, entity.getGpuJson());
        }
        if (entity.getCpuUsagePerCoreJson() == null) {
          statement.bindNull(21);
        } else {
          statement.bindString(21, entity.getCpuUsagePerCoreJson());
        }
        if (entity.getCpuFreqPerCoreJson() == null) {
          statement.bindNull(22);
        } else {
          statement.bindString(22, entity.getCpuFreqPerCoreJson());
        }
        if (entity.getCpuFrequency() == null) {
          statement.bindNull(23);
        } else {
          statement.bindDouble(23, entity.getCpuFrequency());
        }
        if (entity.getCpuTemp() == null) {
          statement.bindNull(24);
        } else {
          statement.bindDouble(24, entity.getCpuTemp());
        }
        if (entity.getDiskIoJson() == null) {
          statement.bindNull(25);
        } else {
          statement.bindString(25, entity.getDiskIoJson());
        }
      }
    };
    this.__deletionAdapterOfDeviceEntity = new EntityDeletionOrUpdateAdapter<DeviceEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `devices` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DeviceEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateDeviceConfig = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE devices SET name = ?, ipAddress = ?, port = ?, token = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM devices";
        return _query;
      }
    };
  }

  @Override
  public Object insertDevice(final DeviceEntity device,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDeviceEntity.insert(device);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteDevice(final DeviceEntity device,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfDeviceEntity.handle(device);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateDeviceConfig(final String id, final String name, final String ip,
      final int port, final String token, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateDeviceConfig.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, name);
        _argIndex = 2;
        _stmt.bindString(_argIndex, ip);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, port);
        _argIndex = 4;
        if (token == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, token);
        }
        _argIndex = 5;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateDeviceConfig.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<DeviceEntity>> getAllDevices() {
    final String _sql = "SELECT * FROM devices";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"devices"}, new Callable<List<DeviceEntity>>() {
      @Override
      @NonNull
      public List<DeviceEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIpAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "ipAddress");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfToken = CursorUtil.getColumnIndexOrThrow(_cursor, "token");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCpuUsage = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuUsage");
          final int _cursorIndexOfRamUsage = CursorUtil.getColumnIndexOrThrow(_cursor, "ramUsage");
          final int _cursorIndexOfDiskUsage = CursorUtil.getColumnIndexOrThrow(_cursor, "diskUsage");
          final int _cursorIndexOfLatency = CursorUtil.getColumnIndexOrThrow(_cursor, "latency");
          final int _cursorIndexOfUptime = CursorUtil.getColumnIndexOrThrow(_cursor, "uptime");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfProcessesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "processesJson");
          final int _cursorIndexOfHardwareJson = CursorUtil.getColumnIndexOrThrow(_cursor, "hardwareJson");
          final int _cursorIndexOfSystemJson = CursorUtil.getColumnIndexOrThrow(_cursor, "systemJson");
          final int _cursorIndexOfBatteryJson = CursorUtil.getColumnIndexOrThrow(_cursor, "batteryJson");
          final int _cursorIndexOfNetworkJson = CursorUtil.getColumnIndexOrThrow(_cursor, "networkJson");
          final int _cursorIndexOfDiskJson = CursorUtil.getColumnIndexOrThrow(_cursor, "diskJson");
          final int _cursorIndexOfAppsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "appsJson");
          final int _cursorIndexOfGpuJson = CursorUtil.getColumnIndexOrThrow(_cursor, "gpuJson");
          final int _cursorIndexOfCpuUsagePerCoreJson = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuUsagePerCoreJson");
          final int _cursorIndexOfCpuFreqPerCoreJson = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuFreqPerCoreJson");
          final int _cursorIndexOfCpuFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuFrequency");
          final int _cursorIndexOfCpuTemp = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuTemp");
          final int _cursorIndexOfDiskIoJson = CursorUtil.getColumnIndexOrThrow(_cursor, "diskIoJson");
          final List<DeviceEntity> _result = new ArrayList<DeviceEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DeviceEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpIpAddress;
            _tmpIpAddress = _cursor.getString(_cursorIndexOfIpAddress);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final String _tmpToken;
            if (_cursor.isNull(_cursorIndexOfToken)) {
              _tmpToken = null;
            } else {
              _tmpToken = _cursor.getString(_cursorIndexOfToken);
            }
            final DeviceStatus _tmpStatus;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toDeviceStatus(_tmp);
            final int _tmpCpuUsage;
            _tmpCpuUsage = _cursor.getInt(_cursorIndexOfCpuUsage);
            final int _tmpRamUsage;
            _tmpRamUsage = _cursor.getInt(_cursorIndexOfRamUsage);
            final Integer _tmpDiskUsage;
            if (_cursor.isNull(_cursorIndexOfDiskUsage)) {
              _tmpDiskUsage = null;
            } else {
              _tmpDiskUsage = _cursor.getInt(_cursorIndexOfDiskUsage);
            }
            final long _tmpLatency;
            _tmpLatency = _cursor.getLong(_cursorIndexOfLatency);
            final String _tmpUptime;
            _tmpUptime = _cursor.getString(_cursorIndexOfUptime);
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            final String _tmpProcessesJson;
            if (_cursor.isNull(_cursorIndexOfProcessesJson)) {
              _tmpProcessesJson = null;
            } else {
              _tmpProcessesJson = _cursor.getString(_cursorIndexOfProcessesJson);
            }
            final String _tmpHardwareJson;
            if (_cursor.isNull(_cursorIndexOfHardwareJson)) {
              _tmpHardwareJson = null;
            } else {
              _tmpHardwareJson = _cursor.getString(_cursorIndexOfHardwareJson);
            }
            final String _tmpSystemJson;
            if (_cursor.isNull(_cursorIndexOfSystemJson)) {
              _tmpSystemJson = null;
            } else {
              _tmpSystemJson = _cursor.getString(_cursorIndexOfSystemJson);
            }
            final String _tmpBatteryJson;
            if (_cursor.isNull(_cursorIndexOfBatteryJson)) {
              _tmpBatteryJson = null;
            } else {
              _tmpBatteryJson = _cursor.getString(_cursorIndexOfBatteryJson);
            }
            final String _tmpNetworkJson;
            if (_cursor.isNull(_cursorIndexOfNetworkJson)) {
              _tmpNetworkJson = null;
            } else {
              _tmpNetworkJson = _cursor.getString(_cursorIndexOfNetworkJson);
            }
            final String _tmpDiskJson;
            if (_cursor.isNull(_cursorIndexOfDiskJson)) {
              _tmpDiskJson = null;
            } else {
              _tmpDiskJson = _cursor.getString(_cursorIndexOfDiskJson);
            }
            final String _tmpAppsJson;
            if (_cursor.isNull(_cursorIndexOfAppsJson)) {
              _tmpAppsJson = null;
            } else {
              _tmpAppsJson = _cursor.getString(_cursorIndexOfAppsJson);
            }
            final String _tmpGpuJson;
            if (_cursor.isNull(_cursorIndexOfGpuJson)) {
              _tmpGpuJson = null;
            } else {
              _tmpGpuJson = _cursor.getString(_cursorIndexOfGpuJson);
            }
            final String _tmpCpuUsagePerCoreJson;
            if (_cursor.isNull(_cursorIndexOfCpuUsagePerCoreJson)) {
              _tmpCpuUsagePerCoreJson = null;
            } else {
              _tmpCpuUsagePerCoreJson = _cursor.getString(_cursorIndexOfCpuUsagePerCoreJson);
            }
            final String _tmpCpuFreqPerCoreJson;
            if (_cursor.isNull(_cursorIndexOfCpuFreqPerCoreJson)) {
              _tmpCpuFreqPerCoreJson = null;
            } else {
              _tmpCpuFreqPerCoreJson = _cursor.getString(_cursorIndexOfCpuFreqPerCoreJson);
            }
            final Double _tmpCpuFrequency;
            if (_cursor.isNull(_cursorIndexOfCpuFrequency)) {
              _tmpCpuFrequency = null;
            } else {
              _tmpCpuFrequency = _cursor.getDouble(_cursorIndexOfCpuFrequency);
            }
            final Double _tmpCpuTemp;
            if (_cursor.isNull(_cursorIndexOfCpuTemp)) {
              _tmpCpuTemp = null;
            } else {
              _tmpCpuTemp = _cursor.getDouble(_cursorIndexOfCpuTemp);
            }
            final String _tmpDiskIoJson;
            if (_cursor.isNull(_cursorIndexOfDiskIoJson)) {
              _tmpDiskIoJson = null;
            } else {
              _tmpDiskIoJson = _cursor.getString(_cursorIndexOfDiskIoJson);
            }
            _item = new DeviceEntity(_tmpId,_tmpName,_tmpIpAddress,_tmpPort,_tmpToken,_tmpStatus,_tmpCpuUsage,_tmpRamUsage,_tmpDiskUsage,_tmpLatency,_tmpUptime,_tmpLastSeen,_tmpProcessesJson,_tmpHardwareJson,_tmpSystemJson,_tmpBatteryJson,_tmpNetworkJson,_tmpDiskJson,_tmpAppsJson,_tmpGpuJson,_tmpCpuUsagePerCoreJson,_tmpCpuFreqPerCoreJson,_tmpCpuFrequency,_tmpCpuTemp,_tmpDiskIoJson);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<DeviceEntity> getDeviceById(final String id) {
    final String _sql = "SELECT * FROM devices WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"devices"}, new Callable<DeviceEntity>() {
      @Override
      @Nullable
      public DeviceEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIpAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "ipAddress");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfToken = CursorUtil.getColumnIndexOrThrow(_cursor, "token");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCpuUsage = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuUsage");
          final int _cursorIndexOfRamUsage = CursorUtil.getColumnIndexOrThrow(_cursor, "ramUsage");
          final int _cursorIndexOfDiskUsage = CursorUtil.getColumnIndexOrThrow(_cursor, "diskUsage");
          final int _cursorIndexOfLatency = CursorUtil.getColumnIndexOrThrow(_cursor, "latency");
          final int _cursorIndexOfUptime = CursorUtil.getColumnIndexOrThrow(_cursor, "uptime");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfProcessesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "processesJson");
          final int _cursorIndexOfHardwareJson = CursorUtil.getColumnIndexOrThrow(_cursor, "hardwareJson");
          final int _cursorIndexOfSystemJson = CursorUtil.getColumnIndexOrThrow(_cursor, "systemJson");
          final int _cursorIndexOfBatteryJson = CursorUtil.getColumnIndexOrThrow(_cursor, "batteryJson");
          final int _cursorIndexOfNetworkJson = CursorUtil.getColumnIndexOrThrow(_cursor, "networkJson");
          final int _cursorIndexOfDiskJson = CursorUtil.getColumnIndexOrThrow(_cursor, "diskJson");
          final int _cursorIndexOfAppsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "appsJson");
          final int _cursorIndexOfGpuJson = CursorUtil.getColumnIndexOrThrow(_cursor, "gpuJson");
          final int _cursorIndexOfCpuUsagePerCoreJson = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuUsagePerCoreJson");
          final int _cursorIndexOfCpuFreqPerCoreJson = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuFreqPerCoreJson");
          final int _cursorIndexOfCpuFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuFrequency");
          final int _cursorIndexOfCpuTemp = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuTemp");
          final int _cursorIndexOfDiskIoJson = CursorUtil.getColumnIndexOrThrow(_cursor, "diskIoJson");
          final DeviceEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpIpAddress;
            _tmpIpAddress = _cursor.getString(_cursorIndexOfIpAddress);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final String _tmpToken;
            if (_cursor.isNull(_cursorIndexOfToken)) {
              _tmpToken = null;
            } else {
              _tmpToken = _cursor.getString(_cursorIndexOfToken);
            }
            final DeviceStatus _tmpStatus;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toDeviceStatus(_tmp);
            final int _tmpCpuUsage;
            _tmpCpuUsage = _cursor.getInt(_cursorIndexOfCpuUsage);
            final int _tmpRamUsage;
            _tmpRamUsage = _cursor.getInt(_cursorIndexOfRamUsage);
            final Integer _tmpDiskUsage;
            if (_cursor.isNull(_cursorIndexOfDiskUsage)) {
              _tmpDiskUsage = null;
            } else {
              _tmpDiskUsage = _cursor.getInt(_cursorIndexOfDiskUsage);
            }
            final long _tmpLatency;
            _tmpLatency = _cursor.getLong(_cursorIndexOfLatency);
            final String _tmpUptime;
            _tmpUptime = _cursor.getString(_cursorIndexOfUptime);
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            final String _tmpProcessesJson;
            if (_cursor.isNull(_cursorIndexOfProcessesJson)) {
              _tmpProcessesJson = null;
            } else {
              _tmpProcessesJson = _cursor.getString(_cursorIndexOfProcessesJson);
            }
            final String _tmpHardwareJson;
            if (_cursor.isNull(_cursorIndexOfHardwareJson)) {
              _tmpHardwareJson = null;
            } else {
              _tmpHardwareJson = _cursor.getString(_cursorIndexOfHardwareJson);
            }
            final String _tmpSystemJson;
            if (_cursor.isNull(_cursorIndexOfSystemJson)) {
              _tmpSystemJson = null;
            } else {
              _tmpSystemJson = _cursor.getString(_cursorIndexOfSystemJson);
            }
            final String _tmpBatteryJson;
            if (_cursor.isNull(_cursorIndexOfBatteryJson)) {
              _tmpBatteryJson = null;
            } else {
              _tmpBatteryJson = _cursor.getString(_cursorIndexOfBatteryJson);
            }
            final String _tmpNetworkJson;
            if (_cursor.isNull(_cursorIndexOfNetworkJson)) {
              _tmpNetworkJson = null;
            } else {
              _tmpNetworkJson = _cursor.getString(_cursorIndexOfNetworkJson);
            }
            final String _tmpDiskJson;
            if (_cursor.isNull(_cursorIndexOfDiskJson)) {
              _tmpDiskJson = null;
            } else {
              _tmpDiskJson = _cursor.getString(_cursorIndexOfDiskJson);
            }
            final String _tmpAppsJson;
            if (_cursor.isNull(_cursorIndexOfAppsJson)) {
              _tmpAppsJson = null;
            } else {
              _tmpAppsJson = _cursor.getString(_cursorIndexOfAppsJson);
            }
            final String _tmpGpuJson;
            if (_cursor.isNull(_cursorIndexOfGpuJson)) {
              _tmpGpuJson = null;
            } else {
              _tmpGpuJson = _cursor.getString(_cursorIndexOfGpuJson);
            }
            final String _tmpCpuUsagePerCoreJson;
            if (_cursor.isNull(_cursorIndexOfCpuUsagePerCoreJson)) {
              _tmpCpuUsagePerCoreJson = null;
            } else {
              _tmpCpuUsagePerCoreJson = _cursor.getString(_cursorIndexOfCpuUsagePerCoreJson);
            }
            final String _tmpCpuFreqPerCoreJson;
            if (_cursor.isNull(_cursorIndexOfCpuFreqPerCoreJson)) {
              _tmpCpuFreqPerCoreJson = null;
            } else {
              _tmpCpuFreqPerCoreJson = _cursor.getString(_cursorIndexOfCpuFreqPerCoreJson);
            }
            final Double _tmpCpuFrequency;
            if (_cursor.isNull(_cursorIndexOfCpuFrequency)) {
              _tmpCpuFrequency = null;
            } else {
              _tmpCpuFrequency = _cursor.getDouble(_cursorIndexOfCpuFrequency);
            }
            final Double _tmpCpuTemp;
            if (_cursor.isNull(_cursorIndexOfCpuTemp)) {
              _tmpCpuTemp = null;
            } else {
              _tmpCpuTemp = _cursor.getDouble(_cursorIndexOfCpuTemp);
            }
            final String _tmpDiskIoJson;
            if (_cursor.isNull(_cursorIndexOfDiskIoJson)) {
              _tmpDiskIoJson = null;
            } else {
              _tmpDiskIoJson = _cursor.getString(_cursorIndexOfDiskIoJson);
            }
            _result = new DeviceEntity(_tmpId,_tmpName,_tmpIpAddress,_tmpPort,_tmpToken,_tmpStatus,_tmpCpuUsage,_tmpRamUsage,_tmpDiskUsage,_tmpLatency,_tmpUptime,_tmpLastSeen,_tmpProcessesJson,_tmpHardwareJson,_tmpSystemJson,_tmpBatteryJson,_tmpNetworkJson,_tmpDiskJson,_tmpAppsJson,_tmpGpuJson,_tmpCpuUsagePerCoreJson,_tmpCpuFreqPerCoreJson,_tmpCpuFrequency,_tmpCpuTemp,_tmpDiskIoJson);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getDeviceByIdStatic(final String id,
      final Continuation<? super DeviceEntity> $completion) {
    final String _sql = "SELECT * FROM devices WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DeviceEntity>() {
      @Override
      @Nullable
      public DeviceEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIpAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "ipAddress");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfToken = CursorUtil.getColumnIndexOrThrow(_cursor, "token");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCpuUsage = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuUsage");
          final int _cursorIndexOfRamUsage = CursorUtil.getColumnIndexOrThrow(_cursor, "ramUsage");
          final int _cursorIndexOfDiskUsage = CursorUtil.getColumnIndexOrThrow(_cursor, "diskUsage");
          final int _cursorIndexOfLatency = CursorUtil.getColumnIndexOrThrow(_cursor, "latency");
          final int _cursorIndexOfUptime = CursorUtil.getColumnIndexOrThrow(_cursor, "uptime");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfProcessesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "processesJson");
          final int _cursorIndexOfHardwareJson = CursorUtil.getColumnIndexOrThrow(_cursor, "hardwareJson");
          final int _cursorIndexOfSystemJson = CursorUtil.getColumnIndexOrThrow(_cursor, "systemJson");
          final int _cursorIndexOfBatteryJson = CursorUtil.getColumnIndexOrThrow(_cursor, "batteryJson");
          final int _cursorIndexOfNetworkJson = CursorUtil.getColumnIndexOrThrow(_cursor, "networkJson");
          final int _cursorIndexOfDiskJson = CursorUtil.getColumnIndexOrThrow(_cursor, "diskJson");
          final int _cursorIndexOfAppsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "appsJson");
          final int _cursorIndexOfGpuJson = CursorUtil.getColumnIndexOrThrow(_cursor, "gpuJson");
          final int _cursorIndexOfCpuUsagePerCoreJson = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuUsagePerCoreJson");
          final int _cursorIndexOfCpuFreqPerCoreJson = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuFreqPerCoreJson");
          final int _cursorIndexOfCpuFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuFrequency");
          final int _cursorIndexOfCpuTemp = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuTemp");
          final int _cursorIndexOfDiskIoJson = CursorUtil.getColumnIndexOrThrow(_cursor, "diskIoJson");
          final DeviceEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpIpAddress;
            _tmpIpAddress = _cursor.getString(_cursorIndexOfIpAddress);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final String _tmpToken;
            if (_cursor.isNull(_cursorIndexOfToken)) {
              _tmpToken = null;
            } else {
              _tmpToken = _cursor.getString(_cursorIndexOfToken);
            }
            final DeviceStatus _tmpStatus;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toDeviceStatus(_tmp);
            final int _tmpCpuUsage;
            _tmpCpuUsage = _cursor.getInt(_cursorIndexOfCpuUsage);
            final int _tmpRamUsage;
            _tmpRamUsage = _cursor.getInt(_cursorIndexOfRamUsage);
            final Integer _tmpDiskUsage;
            if (_cursor.isNull(_cursorIndexOfDiskUsage)) {
              _tmpDiskUsage = null;
            } else {
              _tmpDiskUsage = _cursor.getInt(_cursorIndexOfDiskUsage);
            }
            final long _tmpLatency;
            _tmpLatency = _cursor.getLong(_cursorIndexOfLatency);
            final String _tmpUptime;
            _tmpUptime = _cursor.getString(_cursorIndexOfUptime);
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            final String _tmpProcessesJson;
            if (_cursor.isNull(_cursorIndexOfProcessesJson)) {
              _tmpProcessesJson = null;
            } else {
              _tmpProcessesJson = _cursor.getString(_cursorIndexOfProcessesJson);
            }
            final String _tmpHardwareJson;
            if (_cursor.isNull(_cursorIndexOfHardwareJson)) {
              _tmpHardwareJson = null;
            } else {
              _tmpHardwareJson = _cursor.getString(_cursorIndexOfHardwareJson);
            }
            final String _tmpSystemJson;
            if (_cursor.isNull(_cursorIndexOfSystemJson)) {
              _tmpSystemJson = null;
            } else {
              _tmpSystemJson = _cursor.getString(_cursorIndexOfSystemJson);
            }
            final String _tmpBatteryJson;
            if (_cursor.isNull(_cursorIndexOfBatteryJson)) {
              _tmpBatteryJson = null;
            } else {
              _tmpBatteryJson = _cursor.getString(_cursorIndexOfBatteryJson);
            }
            final String _tmpNetworkJson;
            if (_cursor.isNull(_cursorIndexOfNetworkJson)) {
              _tmpNetworkJson = null;
            } else {
              _tmpNetworkJson = _cursor.getString(_cursorIndexOfNetworkJson);
            }
            final String _tmpDiskJson;
            if (_cursor.isNull(_cursorIndexOfDiskJson)) {
              _tmpDiskJson = null;
            } else {
              _tmpDiskJson = _cursor.getString(_cursorIndexOfDiskJson);
            }
            final String _tmpAppsJson;
            if (_cursor.isNull(_cursorIndexOfAppsJson)) {
              _tmpAppsJson = null;
            } else {
              _tmpAppsJson = _cursor.getString(_cursorIndexOfAppsJson);
            }
            final String _tmpGpuJson;
            if (_cursor.isNull(_cursorIndexOfGpuJson)) {
              _tmpGpuJson = null;
            } else {
              _tmpGpuJson = _cursor.getString(_cursorIndexOfGpuJson);
            }
            final String _tmpCpuUsagePerCoreJson;
            if (_cursor.isNull(_cursorIndexOfCpuUsagePerCoreJson)) {
              _tmpCpuUsagePerCoreJson = null;
            } else {
              _tmpCpuUsagePerCoreJson = _cursor.getString(_cursorIndexOfCpuUsagePerCoreJson);
            }
            final String _tmpCpuFreqPerCoreJson;
            if (_cursor.isNull(_cursorIndexOfCpuFreqPerCoreJson)) {
              _tmpCpuFreqPerCoreJson = null;
            } else {
              _tmpCpuFreqPerCoreJson = _cursor.getString(_cursorIndexOfCpuFreqPerCoreJson);
            }
            final Double _tmpCpuFrequency;
            if (_cursor.isNull(_cursorIndexOfCpuFrequency)) {
              _tmpCpuFrequency = null;
            } else {
              _tmpCpuFrequency = _cursor.getDouble(_cursorIndexOfCpuFrequency);
            }
            final Double _tmpCpuTemp;
            if (_cursor.isNull(_cursorIndexOfCpuTemp)) {
              _tmpCpuTemp = null;
            } else {
              _tmpCpuTemp = _cursor.getDouble(_cursorIndexOfCpuTemp);
            }
            final String _tmpDiskIoJson;
            if (_cursor.isNull(_cursorIndexOfDiskIoJson)) {
              _tmpDiskIoJson = null;
            } else {
              _tmpDiskIoJson = _cursor.getString(_cursorIndexOfDiskIoJson);
            }
            _result = new DeviceEntity(_tmpId,_tmpName,_tmpIpAddress,_tmpPort,_tmpToken,_tmpStatus,_tmpCpuUsage,_tmpRamUsage,_tmpDiskUsage,_tmpLatency,_tmpUptime,_tmpLastSeen,_tmpProcessesJson,_tmpHardwareJson,_tmpSystemJson,_tmpBatteryJson,_tmpNetworkJson,_tmpDiskJson,_tmpAppsJson,_tmpGpuJson,_tmpCpuUsagePerCoreJson,_tmpCpuFreqPerCoreJson,_tmpCpuFrequency,_tmpCpuTemp,_tmpDiskIoJson);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getDeviceByName(final String name,
      final Continuation<? super DeviceEntity> $completion) {
    final String _sql = "SELECT * FROM devices WHERE name = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, name);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DeviceEntity>() {
      @Override
      @Nullable
      public DeviceEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIpAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "ipAddress");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfToken = CursorUtil.getColumnIndexOrThrow(_cursor, "token");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCpuUsage = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuUsage");
          final int _cursorIndexOfRamUsage = CursorUtil.getColumnIndexOrThrow(_cursor, "ramUsage");
          final int _cursorIndexOfDiskUsage = CursorUtil.getColumnIndexOrThrow(_cursor, "diskUsage");
          final int _cursorIndexOfLatency = CursorUtil.getColumnIndexOrThrow(_cursor, "latency");
          final int _cursorIndexOfUptime = CursorUtil.getColumnIndexOrThrow(_cursor, "uptime");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfProcessesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "processesJson");
          final int _cursorIndexOfHardwareJson = CursorUtil.getColumnIndexOrThrow(_cursor, "hardwareJson");
          final int _cursorIndexOfSystemJson = CursorUtil.getColumnIndexOrThrow(_cursor, "systemJson");
          final int _cursorIndexOfBatteryJson = CursorUtil.getColumnIndexOrThrow(_cursor, "batteryJson");
          final int _cursorIndexOfNetworkJson = CursorUtil.getColumnIndexOrThrow(_cursor, "networkJson");
          final int _cursorIndexOfDiskJson = CursorUtil.getColumnIndexOrThrow(_cursor, "diskJson");
          final int _cursorIndexOfAppsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "appsJson");
          final int _cursorIndexOfGpuJson = CursorUtil.getColumnIndexOrThrow(_cursor, "gpuJson");
          final int _cursorIndexOfCpuUsagePerCoreJson = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuUsagePerCoreJson");
          final int _cursorIndexOfCpuFreqPerCoreJson = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuFreqPerCoreJson");
          final int _cursorIndexOfCpuFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuFrequency");
          final int _cursorIndexOfCpuTemp = CursorUtil.getColumnIndexOrThrow(_cursor, "cpuTemp");
          final int _cursorIndexOfDiskIoJson = CursorUtil.getColumnIndexOrThrow(_cursor, "diskIoJson");
          final DeviceEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpIpAddress;
            _tmpIpAddress = _cursor.getString(_cursorIndexOfIpAddress);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final String _tmpToken;
            if (_cursor.isNull(_cursorIndexOfToken)) {
              _tmpToken = null;
            } else {
              _tmpToken = _cursor.getString(_cursorIndexOfToken);
            }
            final DeviceStatus _tmpStatus;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toDeviceStatus(_tmp);
            final int _tmpCpuUsage;
            _tmpCpuUsage = _cursor.getInt(_cursorIndexOfCpuUsage);
            final int _tmpRamUsage;
            _tmpRamUsage = _cursor.getInt(_cursorIndexOfRamUsage);
            final Integer _tmpDiskUsage;
            if (_cursor.isNull(_cursorIndexOfDiskUsage)) {
              _tmpDiskUsage = null;
            } else {
              _tmpDiskUsage = _cursor.getInt(_cursorIndexOfDiskUsage);
            }
            final long _tmpLatency;
            _tmpLatency = _cursor.getLong(_cursorIndexOfLatency);
            final String _tmpUptime;
            _tmpUptime = _cursor.getString(_cursorIndexOfUptime);
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            final String _tmpProcessesJson;
            if (_cursor.isNull(_cursorIndexOfProcessesJson)) {
              _tmpProcessesJson = null;
            } else {
              _tmpProcessesJson = _cursor.getString(_cursorIndexOfProcessesJson);
            }
            final String _tmpHardwareJson;
            if (_cursor.isNull(_cursorIndexOfHardwareJson)) {
              _tmpHardwareJson = null;
            } else {
              _tmpHardwareJson = _cursor.getString(_cursorIndexOfHardwareJson);
            }
            final String _tmpSystemJson;
            if (_cursor.isNull(_cursorIndexOfSystemJson)) {
              _tmpSystemJson = null;
            } else {
              _tmpSystemJson = _cursor.getString(_cursorIndexOfSystemJson);
            }
            final String _tmpBatteryJson;
            if (_cursor.isNull(_cursorIndexOfBatteryJson)) {
              _tmpBatteryJson = null;
            } else {
              _tmpBatteryJson = _cursor.getString(_cursorIndexOfBatteryJson);
            }
            final String _tmpNetworkJson;
            if (_cursor.isNull(_cursorIndexOfNetworkJson)) {
              _tmpNetworkJson = null;
            } else {
              _tmpNetworkJson = _cursor.getString(_cursorIndexOfNetworkJson);
            }
            final String _tmpDiskJson;
            if (_cursor.isNull(_cursorIndexOfDiskJson)) {
              _tmpDiskJson = null;
            } else {
              _tmpDiskJson = _cursor.getString(_cursorIndexOfDiskJson);
            }
            final String _tmpAppsJson;
            if (_cursor.isNull(_cursorIndexOfAppsJson)) {
              _tmpAppsJson = null;
            } else {
              _tmpAppsJson = _cursor.getString(_cursorIndexOfAppsJson);
            }
            final String _tmpGpuJson;
            if (_cursor.isNull(_cursorIndexOfGpuJson)) {
              _tmpGpuJson = null;
            } else {
              _tmpGpuJson = _cursor.getString(_cursorIndexOfGpuJson);
            }
            final String _tmpCpuUsagePerCoreJson;
            if (_cursor.isNull(_cursorIndexOfCpuUsagePerCoreJson)) {
              _tmpCpuUsagePerCoreJson = null;
            } else {
              _tmpCpuUsagePerCoreJson = _cursor.getString(_cursorIndexOfCpuUsagePerCoreJson);
            }
            final String _tmpCpuFreqPerCoreJson;
            if (_cursor.isNull(_cursorIndexOfCpuFreqPerCoreJson)) {
              _tmpCpuFreqPerCoreJson = null;
            } else {
              _tmpCpuFreqPerCoreJson = _cursor.getString(_cursorIndexOfCpuFreqPerCoreJson);
            }
            final Double _tmpCpuFrequency;
            if (_cursor.isNull(_cursorIndexOfCpuFrequency)) {
              _tmpCpuFrequency = null;
            } else {
              _tmpCpuFrequency = _cursor.getDouble(_cursorIndexOfCpuFrequency);
            }
            final Double _tmpCpuTemp;
            if (_cursor.isNull(_cursorIndexOfCpuTemp)) {
              _tmpCpuTemp = null;
            } else {
              _tmpCpuTemp = _cursor.getDouble(_cursorIndexOfCpuTemp);
            }
            final String _tmpDiskIoJson;
            if (_cursor.isNull(_cursorIndexOfDiskIoJson)) {
              _tmpDiskIoJson = null;
            } else {
              _tmpDiskIoJson = _cursor.getString(_cursorIndexOfDiskIoJson);
            }
            _result = new DeviceEntity(_tmpId,_tmpName,_tmpIpAddress,_tmpPort,_tmpToken,_tmpStatus,_tmpCpuUsage,_tmpRamUsage,_tmpDiskUsage,_tmpLatency,_tmpUptime,_tmpLastSeen,_tmpProcessesJson,_tmpHardwareJson,_tmpSystemJson,_tmpBatteryJson,_tmpNetworkJson,_tmpDiskJson,_tmpAppsJson,_tmpGpuJson,_tmpCpuUsagePerCoreJson,_tmpCpuFreqPerCoreJson,_tmpCpuFrequency,_tmpCpuTemp,_tmpDiskIoJson);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

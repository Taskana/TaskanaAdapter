package pro.taskana.adapter.manager;

import java.io.Closeable;
import java.io.IOException;

import org.apache.ibatis.session.SqlSessionManager;

public class AdapterConnection implements Closeable {

    private AdapterManager adapterManager;
    private SqlSessionManager sqlSessionManager;

    AdapterConnection(AdapterManager adapterManager, SqlSessionManager sqlSessionManager) {
        this.adapterManager = adapterManager;
        this.sqlSessionManager = sqlSessionManager;
        adapterManager.openConnection(sqlSessionManager);
    }

    @Override
    public void close() throws IOException {
        adapterManager.returnConnection(sqlSessionManager);

    }

}

package org.skywalking.apm.plugin.mongodb.v3;

import com.mongodb.ServerAddress;
import com.mongodb.binding.ConnectionSource;
import com.mongodb.binding.WriteBinding;
import com.mongodb.connection.ServerConnectionState;
import com.mongodb.connection.ServerDescription;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.skywalking.apm.agent.core.plugin.interceptor.EnhancedClassInstanceContext;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodInvokeContext;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
public class MongoDBWriteBindingInterceptorTest {

    private MongoDBWriteBindingInterceptor interceptor;

    @Mock
    private EnhancedClassInstanceContext instanceContext;

    @Mock
    private InstanceMethodInvokeContext interceptorContext;

    @Mock
    private WriteBinding writeBinding;

    @Mock
    private ConnectionSource connectionSource;

    private ServerAddress address = new ServerAddress("127.0.0.1", 27017);

    @Before
    public void setUp() throws Exception {

        interceptor = new MongoDBWriteBindingInterceptor();

        ServerDescription serverDescription =
            ServerDescription.builder().address(address).state(ServerConnectionState.CONNECTED).build();

        PowerMockito.when(connectionSource.getServerDescription()).thenReturn(serverDescription);

        PowerMockito.when(writeBinding.getWriteConnectionSource()).thenReturn(connectionSource);

    }

    @Test
    public void afterMethodTest() throws Exception {
        interceptor.afterMethod(instanceContext, interceptorContext, writeBinding);
        verify(instanceContext, times(1)).set(MongoDBMethodInterceptor.MONGODB_HOST, "127.0.0.1");
        verify(instanceContext, times(1)).set(MongoDBMethodInterceptor.MONGODB_PORT, 27017);
    }

}

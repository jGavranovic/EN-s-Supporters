package com.example.ticketapp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.view.Menu;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MainActivityTest {

    @After
    public void tearDown() {
        TestSessionUtils.resetUserSessionSingleton();
    }

    @Test
    public void setupNavigation_inflatesGuestMenuForGuestUser() throws Exception {
        MainActivity activity = new MainActivity();
        BottomNavigationView nav = mock(BottomNavigationView.class);
        Menu menu = mock(Menu.class);
        when(nav.getMenu()).thenReturn(menu);

        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.GUEST);

        setField(activity, "bottomNav", nav);
        setField(activity, "userSession", session);

        invokeSetupNavigation(activity);

        verify(menu).clear();
        verify(nav).inflateMenu(R.menu.bottom_nav_guest);
    }

    @Test
    public void setupNavigation_inflatesAdminMenuForAdminUser() throws Exception {
        MainActivity activity = new MainActivity();
        BottomNavigationView nav = mock(BottomNavigationView.class);
        Menu menu = mock(Menu.class);
        when(nav.getMenu()).thenReturn(menu);

        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.ADMIN);

        setField(activity, "bottomNav", nav);
        setField(activity, "userSession", session);

        invokeSetupNavigation(activity);

        verify(menu).clear();
        verify(nav).inflateMenu(R.menu.bottom_nav_admin);
    }

    @Test
    public void setupNavigation_inflatesUserMenuForStandardUser() throws Exception {
        MainActivity activity = new MainActivity();
        BottomNavigationView nav = mock(BottomNavigationView.class);
        Menu menu = mock(Menu.class);
        when(nav.getMenu()).thenReturn(menu);

        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.USER);

        setField(activity, "bottomNav", nav);
        setField(activity, "userSession", session);

        invokeSetupNavigation(activity);

        verify(menu).clear();
        verify(nav).inflateMenu(R.menu.bottom_nav_user);
    }

    private void invokeSetupNavigation(MainActivity activity) throws Exception {
        Method method = MainActivity.class.getDeclaredMethod("setupNavigation");
        method.setAccessible(true);
        method.invoke(activity);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}




package swingtree;

import org.jspecify.annotations.Nullable;

import javax.swing.ComboBoxEditor;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.plaf.UIResource;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.List;
import java.util.Objects;

final class InternalComboBoxCellEditor implements ComboBoxEditor,FocusListener {

    private final List<ActionListener> _actionListeners = new java.util.ArrayList<>();
    /**
     * An instance of {@code JTextField}.
     */
    private JTextField editor;
    private @Nullable Object oldValue;

    /**
     * Constructs a new instance of {@code BasicComboBoxEditor}.
     */
    public InternalComboBoxCellEditor(@Nullable JTextField editor) {
        this.editor = editor == null ? createEditorComponent() : editor;
    }

    @Override
    public Component getEditorComponent() {
        return editor;
    }

    public void setEditorComponent(JTextField editor) {
        for (ActionListener l : _actionListeners) {
            this.editor.removeActionListener(l);
            editor.addActionListener(l);
        }
        this.editor = editor;
    }

    /**
     * Creates the internal editor component. Override this to provide
     * a custom implementation.
     *
     * @return a new editor component
     */
    private JTextField createEditorComponent() {
        JTextField editor = new BorderlessTextField("",9);
        editor.setBorder(null);
        return editor;
    }

    /**
     * Sets the item that should be edited.
     *
     * @param anObject the displayed value of the editor
     */
    @Override
    public void setItem(Object anObject) {
        String text;

        if ( anObject != null )  {
            text = anObject.toString();
            if (text == null) {
                text = "";
            }
            oldValue = anObject;
        } else {
            text = "";
        }
        // workaround for 4530952
        if (! text.equals(editor.getText())) {
            editor.setText(text);
        }
    }

    @Override
    public Object getItem() {
        Object newValue = editor.getText();

        if (oldValue != null && !(oldValue instanceof String))  {
            // The original value is not a string. Should return the value in it's
            // original type.
            if (newValue.equals(oldValue.toString()))  {
                return oldValue;
            } else {
                // Must take the value from the editor and get the value and cast it to the new type.
                Class<?> cls = oldValue.getClass();
                try {
                    Method method = MethodUtil.getMethod(cls, "valueOf", new Class<?>[]{String.class});
                    newValue = MethodUtil.invoke(method, oldValue, new Object[] { editor.getText()});
                } catch (Exception ex) {
                    // Fail silently and return the newValue (a String object)
                }
            }
        }
        return newValue;
    }

    @Override
    public void selectAll() {
        editor.selectAll();
        editor.requestFocus();
    }

    // This used to do something but now it doesn't.  It couldn't be
    // removed because it would be an API change to do so.
    @Override
    public void focusGained(FocusEvent e) {}

    // This used to do something but now it doesn't.  It couldn't be
    // removed because it would be an API change to do so.
    @Override
    public void focusLost(FocusEvent e) {}

    @Override
    public void addActionListener(ActionListener l) {
        _actionListeners.add(l);
        editor.addActionListener(l);
    }

    @Override
    public void removeActionListener(ActionListener l) {
        _actionListeners.remove(l);
        editor.removeActionListener(l);
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    static class BorderlessTextField extends JTextField {
        public BorderlessTextField(String value,int n) {
            super(value,n);
        }

        // workaround for 4530952
        public void setText(String s) {
            if (getText().equals(s)) {
                return;
            }
            super.setText(s);
        }

        public void setBorder(Border b) {
            if (!(b instanceof UIResource)) {
                super.setBorder(b);
            }
        }
    }

    /*
     * Create a trampoline class.
     */
    static class MethodUtil extends SecureClassLoader {
        private static final String MISC_PKG = "sun.reflect.misc.";
        private static final String TRAMPOLINE = MISC_PKG + "Trampoline";
        private static final Method bounce = getTrampoline();

        private MethodUtil() {
            super();
        }

        /*
         * Bounce through the trampoline.
         */
        public static Object invoke(Method m, Object obj, Object[] params)
                throws InvocationTargetException, IllegalAccessException {
            try {
                return bounce.invoke(null, new Object[] {m, obj, params});
            } catch (InvocationTargetException ie) {
                Throwable t = ie.getCause();

                if (t instanceof InvocationTargetException) {
                    throw (InvocationTargetException)t;
                } else if (t instanceof IllegalAccessException) {
                    throw (IllegalAccessException)t;
                } else if (t instanceof RuntimeException) {
                    throw (RuntimeException)t;
                } else if (t instanceof Error) {
                    throw (Error)t;
                } else {
                    throw new Error("Unexpected invocation error", t);
                }
            } catch (IllegalAccessException iae) {
                // this can't happen
                throw new Error("Unexpected invocation error", iae);
            }
        }

        public static Method getMethod(Class<?> cls, String name, Class<?>[] args)
            throws NoSuchMethodException {
            ReflectUtil.checkPackageAccess(cls);
            return cls.getMethod(name, args);
        }

        private static Method getTrampoline() {
            try {
                return AccessController.doPrivileged(
                        new PrivilegedExceptionAction<Method>() {
                            public Method run() throws Exception {
                                Class<?> t = getTrampolineClass();
                                Class<?>[] types = {
                                        Method.class, Object.class, Object[].class
                                };
                                Objects.requireNonNull(t, "Trampoline must be found");
                                Method b = t.getDeclaredMethod("invoke", types);
                                b.setAccessible(true);
                                return b;
                            }
                        });
            } catch (Exception e) {
                throw new InternalError("bouncer cannot be found", e);
            }
        }

        private static @Nullable Class<?> getTrampolineClass() {
            try {
                return Class.forName(TRAMPOLINE, true, new MethodUtil());
            } catch (ClassNotFoundException e) {
            }
            return null;
        }

    }

    static class ReflectUtil {

        private ReflectUtil() {
        }

        /**
         * Checks package access on the given class.
         * <p>
         * If it is a {@link Proxy#isProxyClass(java.lang.Class)} that implements
         * a non-public interface (i.e. may be in a non-restricted package),
         * also check the package access on the proxy interfaces.
         */
        public static void checkPackageAccess(Class<?> clazz) {
            SecurityManager s = System.getSecurityManager();
            if (s != null) {
                privateCheckPackageAccess(s, clazz);
            }
        }

        /**
         * NOTE: should only be called if a SecurityManager is installed
         */
        private static void privateCheckPackageAccess(SecurityManager s, Class<?> clazz) {
            while (clazz.isArray()) {
                clazz = clazz.getComponentType();
            }

            String pkg = clazz.getPackageName();
            if (pkg != null && !pkg.isEmpty()) {
                s.checkPackageAccess(pkg);
            }

            if (isNonPublicProxyClass(clazz)) {
                privateCheckProxyPackageAccess(s, clazz);
            }
        }

        /**
         * NOTE: should only be called if a SecurityManager is installed
         */
        private static void privateCheckProxyPackageAccess(SecurityManager s, Class<?> clazz) {
            // check proxy interfaces if the given class is a proxy class
            if (Proxy.isProxyClass(clazz)) {
                for (Class<?> intf : clazz.getInterfaces()) {
                    privateCheckPackageAccess(s, intf);
                }
            }
        }


        // Note that bytecode instrumentation tools may exclude 'sun.*'
        // classes but not generated proxy classes and so keep it in com.sun.*
        public static final String PROXY_PACKAGE = "com.sun.proxy";

        /**
         * Test if the given class is a proxy class that implements
         * non-public interface.  Such proxy class may be in a non-restricted
         * package that bypasses checkPackageAccess.
         */
        public static boolean isNonPublicProxyClass(Class<?> cls) {
            if (!Proxy.isProxyClass(cls)) {
                return false;
            }
            String pkg = cls.getPackageName();
            return pkg == null || !pkg.startsWith(PROXY_PACKAGE);
        }
    }

}

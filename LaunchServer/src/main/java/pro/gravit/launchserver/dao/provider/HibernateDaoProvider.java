package pro.gravit.launchserver.dao.provider;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.Reconfigurable;
import pro.gravit.launchserver.dao.User;
import pro.gravit.launchserver.dao.impl.UserHibernateImpl;
import pro.gravit.launchserver.dao.impl.HibernateUserDAOImpl;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.LogHelper;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class HibernateDaoProvider extends DaoProvider implements Reconfigurable {
    public String driver;
    public String url;
    public String username;
    public String password;
    public String dialect;
    public String pool_size;
    public String hibernateConfig;
    public boolean parallelHibernateInit;
    private transient SessionFactory sessionFactory;

    @Override
    public void init(LaunchServer server) {
        Runnable init = () -> {
            Configuration cfg = new Configuration()
                    .addAnnotatedClass(UserHibernateImpl.class)
                    .setProperty("hibernate.connection.driver_class", driver)
                    .setProperty("hibernate.connection.url", url)
                    .setProperty("hibernate.connection.username", username)
                    .setProperty("hibernate.connection.password", password)
                    .setProperty("hibernate.connection.pool_size", pool_size);
            if (dialect != null)
                cfg.setProperty("hibernate.dialect", dialect);
            if (hibernateConfig != null)
                cfg.configure(Paths.get(hibernateConfig).toFile());
            sessionFactory = cfg.buildSessionFactory();
            userDAO = new HibernateUserDAOImpl(sessionFactory);
        };
        if (parallelHibernateInit)
            CommonHelper.newThread("Hibernate Thread", true, init);
        else
            init.run();
    }

    @Override
    public Map<String, Command> getCommands() {
        Map<String, Command> commands = new HashMap<>();
        commands.put("getallusers", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                int count = 0;
                for (User user : userDAO.findAll()) {
                    LogHelper.subInfo("[%s] UUID: %s", user.getUsername(), user.getUuid().toString());
                    count++;
                }
                LogHelper.info("Print %d users", count);
            }
        });
        commands.put("getuser", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 1);
                User user = userDAO.findByUsername(args[0]);
                if (user == null) {
                    LogHelper.error("User %s not found", args[0]);
                    return;
                }
                LogHelper.info("[%s] UUID: %s | permissions %s", user.getUsername(), user.getUuid().toString(), user.getPermissions() == null ? "null" : user.getPermissions().toString());
            }
        });
        commands.put("givepermission", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 3);
                User user = userDAO.findByUsername(args[0]);
                if (user == null) {
                    LogHelper.error("User %s not found", args[0]);
                    return;
                }
                ClientPermissions permissions = user.getPermissions();
                long perm = Long.parseLong(args[1]);
                boolean value = Boolean.parseBoolean(args[2]);
                permissions.setPermission(perm, value);
                userDAO.update(user);
            }
        });
        commands.put("giveflag", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 3);
                User user = userDAO.findByUsername(args[0]);
                if (user == null) {
                    LogHelper.error("User %s not found", args[0]);
                    return;
                }
                ClientPermissions permissions = user.getPermissions();
                long perm = Long.parseLong(args[1]);
                boolean value = Boolean.parseBoolean(args[2]);
                permissions.setFlag(perm, value);
                userDAO.update(user);
            }
        });
        return commands;
    }
}

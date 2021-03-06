package me.fanjoker.headcreator.managers;

import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import me.fanjoker.headcreator.Main;
import me.fanjoker.headcreator.objects.HCBlock;
import me.fanjoker.headcreator.objects.HCConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class HCManager {

    private Main main;

    public HCManager(Main main) {
        this.main = main;
    }

    private Connection getConnection() {
        return main.getConnection().getConnection();
    }

//
//    SQL ACTIONS
//

    final String createQuery = "INSERT INTO `HeadCreator` (`location`, `type`, `toggle`) VALUES(?,?,?)";
    public void create(Location location, String type) {

        try (PreparedStatement stm = getConnection().prepareStatement(createQuery)) {
            stm.setString(1, serialize(location));
            stm.setString(2, type);
            stm.setBoolean(3, true);
            stm.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        HCBlock block = new HCBlock(location, main.getSettings().getType(type),true);
        main.getConstructor().getMap().put(getSQLId(location), block);
    }

    final String loadQuery = "SELECT * FROM `HeadCreator`";
    public void loadHeadDatabase() {

        try(PreparedStatement stm = getConnection().prepareStatement(loadQuery)) {
            ResultSet rs = stm.executeQuery();

            while (rs.next()) {

                Location loc = deserialize(rs.getString("location"));
                HCConfig hcConfig = main.getSettings().getType(rs.getString("type"));

                if (hcConfig == null) {
                    main.log("");
                    main.log("§cOcorreu um erro ao recarregar a cabeça na localização: " + serialize(loc));
                    main.log("§cParece que não foi encontrado nenhum tipo dela, na configuração.");
                    main.log("§cDeletando a mesma...");
                    main.log("");

                    loc.getBlock().setType(Material.AIR);
                    deleteHead(loc);
                } else {
                    HCBlock hcBlock = new HCBlock(loc, hcConfig, rs.getBoolean("toggle"));
                    main.getConstructor().getMap().put(rs.getInt("id"), hcBlock);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    final String deleteQuery = "DELETE FROM `HeadCreator` WHERE `id` = ?";
    public void deleteHead(Location loc) {
        loc.getBlock().setType(Material.AIR);

        int id = getSQLId(loc);

        try (PreparedStatement stm = getConnection().prepareStatement(deleteQuery)) {
            stm.setInt(1, id);
            stm.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        main.getConstructor().getMap().remove(id);

    }

    public int getSQLId(Location loc) {
        try {
            PreparedStatement stm;
            stm = getConnection().prepareStatement("SELECT `id` FROM `HeadCreator` WHERE `location` = ?");
            stm.setString(1, serialize(loc));
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            rs.close();
            stm.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }


    private String serialize (Location locations) {
        return locations.getWorld().getName() + ";" + locations.getX() + ";" + locations.getY() + ";" + locations.getZ();
    }

    private Location deserialize (String serialized) {
        String[] split = serialized.split(";");
        return new Location(Bukkit.getWorld(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]),
                Double.parseDouble(split[3]));
    }


    final String saveQuery = "UPDATE `HeadCreator` SET `toggle` = ? WHERE id = ?";
    private void saveHead(int id) {
        HCBlock hcBlock = main.getConstructor().getMap().get(id);
        try(PreparedStatement stm = getConnection().prepareStatement(saveQuery)) {

            stm.setBoolean(1, hcBlock.isToggle());
            stm.setInt(2, id);
            stm.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        main.getConstructor().getMap().clear();
    }

    public void saveHeadDatabase() {

        for (int id : main.getConstructor().getMap().keySet()) {
            saveHead(id);
        }
    }
}

package com.foh.contacto_total_web_service.sms_template.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foh.contacto_total_web_service.sms_template.dto.CombosDTO;
import com.foh.contacto_total_web_service.sms_template.dto.Restricciones;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.*;

@Repository
public class ComboRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper om = new ObjectMapper();

    public ComboRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ----------------- helpers JSON seguros -----------------
    private List<String> readList(String json) {
        try {
            if (json == null || json.isEmpty()) return List.of();
            return om.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Error parseando selects_json: " + json, e);
        }
    }

    private Set<String> readSet(String json) {
        try {
            if (json == null || json.isEmpty()) return Set.of();
            return om.readValue(json, new TypeReference<Set<String>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Error parseando condiciones_json: " + json, e);
        }
    }

    private Restricciones readRestricciones(String json) {
        try {
            if (json == null || json.isEmpty()) return new Restricciones(false, false, false, false );
            return om.readValue(json, Restricciones.class);
        } catch (Exception e) {
            throw new RuntimeException("Error parseando restricciones_json: " + json, e);
        }
    }

    private String toJson(Object o) {
        try { return om.writeValueAsString(o); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    // ----------------- mapeo fila -----------------
    private CombosDTO.Response mapRow(ResultSet rs) {
        try {
            CombosDTO.Response r = new CombosDTO.Response();
            r.id = rs.getInt("id");
            r.name = rs.getString("name");
            r.descripcion = rs.getString("descripcion");
            r.plantillaSmsId = rs.getInt("plantilla_sms_id");
            r.plantillaTexto = rs.getString("plantilla_texto"); // <<--- NUEVO
            r.selects = readList(rs.getString("selects_json"));
            r.tramo = rs.getString("tramo");
            r.condiciones = readSet(rs.getString("condiciones_json"));
            r.restricciones = readRestricciones(rs.getString("restricciones_json"));
            r.isActive = rs.getBoolean("is_active");
            r.createdAt = rs.getTimestamp("created_at");
            r.updatedAt = rs.getTimestamp("updated_at");
            r.rangos = readRanges(rs.getString("rangos_json"));
            return r;
        } catch (Exception e) {
            throw new RuntimeException("Error mapeando TEST_SMS_TEMPLATE_COMBO", e);
        }
    }


    // ----------------- CRUD -----------------
    public List<CombosDTO.Response> findAll() {
        String sql = """
      SELECT c.*, p.template AS plantilla_texto
      FROM TEST_SMS_TEMPLATE_COMBO c
      LEFT JOIN plantillasms p ON p.id = c.plantilla_sms_id
      WHERE c.is_active = 1
      ORDER BY c.id DESC
    """;
        return jdbc.query(sql, (rs, i) -> mapRow(rs));
    }

    public Optional<CombosDTO.Response> findById(Integer id) {
        String sql = """
      SELECT c.*, p.template AS plantilla_texto
      FROM TEST_SMS_TEMPLATE_COMBO c
      LEFT JOIN plantillasms p ON p.id = c.plantilla_sms_id
      WHERE c.id=:id
    """;
        var params = new MapSqlParameterSource("id", id);
        var list = jdbc.query(sql, params, (rs, i) -> mapRow(rs));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Integer insert(CombosDTO.CreateRequest req) {
        Integer plantillaId = req.plantillaSmsId;

        // Si no viene id pero sí texto => crear plantilla nueva
        if (plantillaId == null && req.plantillaTexto != null) {
            String sqlPlantilla = """
            INSERT INTO plantillasms (name, template, tipis)
            VALUES (:name, :template, '[]')
        """;
            var paramsPlantilla = new MapSqlParameterSource()
                    .addValue("name", req.plantillaName != null ? req.plantillaName : req.name)
                    .addValue("template", req.plantillaTexto);
            jdbc.update(sqlPlantilla, paramsPlantilla);
            plantillaId = jdbc.getJdbcOperations().queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
        }

        if (plantillaId == null) {
            throw new IllegalArgumentException("Debes pasar plantillaSmsId o plantillaTexto");
        }

        String sql = """
        INSERT INTO TEST_SMS_TEMPLATE_COMBO
          (name, descripcion, plantilla_sms_id, selects_json, tramo, condiciones_json, restricciones_json, rangos_json, is_active)
        VALUES
          (:name, :descripcion, :plantillaSmsId, :selectsJson, :tramo, :condJson, :restrJson, :rangosJson, 1)
    """;
        var params = new MapSqlParameterSource()
                .addValue("name", req.name)
                .addValue("descripcion", req.descripcion)
                .addValue("plantillaSmsId", plantillaId)
                .addValue("selectsJson", toJson(req.selects == null ? List.of() : req.selects))
                .addValue("tramo", req.tramo)
                .addValue("condJson", toJson(req.condiciones == null ? Set.of() : req.condiciones))
                .addValue("restrJson", toJson(req.restricciones == null ? new Restricciones(false,false,false, false) : req.restricciones))
                .addValue("rangosJson", toJson(req.rangos == null ? List.of() : req.rangos));

        jdbc.update(sql, params);
        return jdbc.getJdbcOperations().queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    public int update(CombosDTO.UpdateRequest req) {
        String sql = """
        UPDATE TEST_SMS_TEMPLATE_COMBO SET
          name              = :name,
          descripcion       = :descripcion,
          plantilla_sms_id  = COALESCE(:plantillaSmsId, plantilla_sms_id),
          selects_json      = :selectsJson,
          tramo             = :tramo,
          condiciones_json  = :condJson,
          restricciones_json= :restrJson,
          rangos_json       = :rangosJson,   -- 👈 NUEVO
          is_active         = :isActive
        WHERE id = :id
        """;

        var params = new MapSqlParameterSource()
                .addValue("id", req.id)
                .addValue("name", req.name)
                .addValue("descripcion", req.descripcion)
                // 👇 la clave DEBE llamarse igual que el placeholder (:plantillaSmsId)
                .addValue("plantillaSmsId", req.plantillaSmsId)   // puede ser null; COALESCE conservará el actual
                .addValue("selectsJson", toJson(req.selects == null ? List.of() : req.selects))
                .addValue("tramo", req.tramo)
                .addValue("condJson", toJson(req.condiciones == null ? Set.of() : req.condiciones))
                .addValue("restrJson", toJson(req.restricciones == null ? new Restricciones(false,false,false, false) : req.restricciones))
                .addValue("rangosJson", toJson(req.rangos == null ? List.of() : req.rangos))
                .addValue("isActive", req.isActive == null ? Boolean.TRUE : req.isActive);

        return jdbc.update(sql, params);
    }

    public int delete(Integer id) {
        String sql = "DELETE FROM TEST_SMS_TEMPLATE_COMBO WHERE id=:id";
        return jdbc.update(sql, new MapSqlParameterSource("id", id));
    }

    // opcional: traer template de texto de plantillasms
    public Optional<String> getPlantillaTextoById(Integer plantillaId) {
        String sql = "SELECT template FROM plantillasms WHERE id=:id";
        var list = jdbc.queryForList(sql, new MapSqlParameterSource("id", plantillaId), String.class);
        return list.isEmpty() ? Optional.empty() : Optional.ofNullable(list.get(0));
    }

    public Optional<String> getPlantillaNameById(Integer plantillaId) {
        String sql = "SELECT name FROM plantillasms WHERE id=:id";
        var list = jdbc.queryForList(sql, new MapSqlParameterSource("id", plantillaId), String.class);
        return list.isEmpty() ? Optional.empty() : Optional.ofNullable(list.get(0));
    }


    private List<CombosDTO.RangeFilter> readRanges(String json) {
        try {
            if (json == null || json.isEmpty()) return List.of();
            return om.readValue(json, new TypeReference<List<CombosDTO.RangeFilter>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Error parseando rangos_json: " + json, e);
        }
    }


    public int updatePlantilla(Integer id, String name, String template) {
        String sql = """
        UPDATE plantillasms
           SET name = COALESCE(:name, name),
               template = :template
         WHERE id = :id
    """;
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("name", name)
                .addValue("template", template));
    }

    public Integer insertPlantilla(String name, String template) {
        String sql = """
        INSERT INTO plantillasms (name, template, tipis)
        VALUES (:name, :template, '[]')
    """;
        var p = new MapSqlParameterSource()
                .addValue("name", name)
                .addValue("template", template);
        jdbc.update(sql, p);
        return jdbc.getJdbcOperations().queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

}


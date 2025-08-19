package com.foh.contacto_total_web_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "FTP_CONSULT")
public class FtpConsult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    String telefono;

    String fecha;

    String hora;

    String pathCompleto;
}

package com.foh.contacto_total_web_service.service;

import com.foh.contacto_total_web_service.dto.GenerateMessagesRequest;
import com.foh.contacto_total_web_service.dto.PlantillaSMSRequest;
import com.foh.contacto_total_web_service.dto.PlantillaSMSToUpdateRequest;
import com.foh.contacto_total_web_service.model.PlantillaSMS;

import java.io.File;
import java.util.List;

public interface PlantillaSMSService {
    public abstract PlantillaSMS createPlantillaSMS(PlantillaSMSRequest plantillaSMSRequest);
    public abstract List<PlantillaSMS> getPlantillasSMS();
    public abstract PlantillaSMS getPlantillaById(Integer id);
    public abstract PlantillaSMS getPlantillaByNombre(String nombre);
    public abstract File getFileByPlantillaWithData(GenerateMessagesRequest generateMessagesRequest);
    public abstract File getFileByPlantillaWithData2(GenerateMessagesRequest generateMessagesRequest);
    public abstract PlantillaSMS updatePlantilla(PlantillaSMSToUpdateRequest plantillaSMSToUpdateRequest);
    public abstract void deletePlantilla(Integer id);
}
package com.foh.contacto_total_web_service.plantillaSMS.service;

import com.foh.contacto_total_web_service.sms.dto.GenerateMessagesRequest;
import com.foh.contacto_total_web_service.plantillaSMS.dto.PlantillaSMSRequest;
import com.foh.contacto_total_web_service.plantillaSMS.dto.PlantillaSMSToUpdateRequest;
import com.foh.contacto_total_web_service.plantillaSMS.model.PlantillaSMS;

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
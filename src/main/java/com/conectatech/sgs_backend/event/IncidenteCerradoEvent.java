package com.conectatech.sgs_backend.event;

import com.conectatech.sgs_backend.model.Incidente;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class IncidenteCerradoEvent extends ApplicationEvent {

    private final Incidente incidente;
    private final String resolucionMunicipal;

    public IncidenteCerradoEvent(Object source, Incidente incidente, String resolucionMunicipal) {
        super(source);
        this.incidente = incidente;
        this.resolucionMunicipal = resolucionMunicipal;
    }
}


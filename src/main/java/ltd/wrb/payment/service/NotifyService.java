package ltd.wrb.payment.service;

import ltd.wrb.payment.model.NotifyTask;

public interface NotifyService {
    
    void addTask(NotifyTask task, boolean runImmidately);

}

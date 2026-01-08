package com.alfarays.mail.service;

import com.alfarays.mail.model.FitVerseMailRequest;

public interface IMailService {

    boolean send(FitVerseMailRequest request);

}

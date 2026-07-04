package com.example.societyhub.service;

import com.example.societyhub.model.Resident;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EmailOrchestrationService {

    private final DBHandler dbHandler;
    private final BillDataAssemblerService assemblerService;
    private final PdfService pdfService;
    private final MailService mailService;

    public EmailOrchestrationService(
            DBHandler dbHandler,
            BillDataAssemblerService assemblerService,
            PdfService pdfService,
            MailService mailService
    ) {
        this.dbHandler = dbHandler;
        this.assemblerService = assemblerService;
        this.pdfService = pdfService;
        this.mailService = mailService;
    }

    /* ===== Monthly Bills ===== */

    public void sendMonthlyBills(int sid) throws Exception {
        com.example.societyhub.model.Bill latestBill = dbHandler.fetchBillDetails(sid);
        String month = (latestBill != null) ? latestBill.getMonth() : "";

        List<Resident> residents = dbHandler.getResident(sid);

        for (Resident resident : residents) {

            Map<String, Object> data =
                    assemblerService.build(
                            resident.getMygate_no(),
                            month,
                            "Pending",
                            sid
                    );

            byte[] pdf =
                    pdfService.generatePdf("admin/final_bill", data);

            mailService.sendEmail(
                    resident.getEmail(),
                    "Monthly Maintenance Bill",
                    "Dear " + resident.getMem_id()
                            + ",\n\nPlease find attached your maintenance bill.",
                    pdf,
                    "Maintenance_Bill.pdf"
            );
        }
    }

    /* ===== Single Receipt ===== */

    public void sendReceipt(
            String mygateNo,
            String month,
            String status,
            int sid,
            String targetEmail
    ) throws Exception {

        Map<String, Object> data =
                assemblerService.build(mygateNo, month, status, sid);
        
        if (targetEmail != null && !targetEmail.trim().isEmpty()) {
            data.put("email", targetEmail);
        } else {
            targetEmail = (String) data.get("email");
        }

        byte[] pdf =
                pdfService.generatePdf("admin/receipt", data);

        mailService.sendEmail(
                targetEmail,
                "Maintenance Receipt",
                "Dear " + data.get("mem_id")
                        + ",\n\nPlease find your receipt attached.",
                pdf,
                "Receipt.pdf"
        );
    }

    /* ===== MyGate ===== */

    public void sendMyGateNumbers(int sid) throws Exception {

        List<Resident> residents = dbHandler.getResident(sid);

        for (Resident resident : residents) {

            String body =
                    "Dear " + resident.getMem_id()
                            + ",\n\nYour MyGate number is: "
                            + resident.getMygate_no();

            mailService.sendEmail(
                    resident.getEmail(),
                    "MyGate Number",
                    body,
                    null,
                    null
            );
        }
    }

    /* ===== Notice ===== */

    public void sendNotice(String message, int sid)
            throws Exception {

        List<Resident> residents = dbHandler.getResident(sid);

        for (Resident resident : residents) {

            String personalized =
                    message.replace("{name}",
                            resident.getMem_id());

            mailService.sendEmail(
                    resident.getEmail(),
                    "Notice",
                    personalized,
                    null,
                    null
            );
        }
    }
}

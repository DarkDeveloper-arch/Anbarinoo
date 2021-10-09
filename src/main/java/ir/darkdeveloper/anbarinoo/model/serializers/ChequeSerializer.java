package ir.darkdeveloper.anbarinoo.model.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import ir.darkdeveloper.anbarinoo.model.Financial.ChequeModel;

import java.io.IOException;

public class ChequeSerializer extends StdSerializer<ChequeModel> {

    public ChequeSerializer() {
        this(null);
    }

    public ChequeSerializer(Class<ChequeModel> t) {
        super(t);
    }

    @Override
    public void serialize(ChequeModel value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        if (value.getId() != null)
            gen.writeNumberField("id", value.getId());
        if (value.getNameOf() != null)
            gen.writeStringField("nameOf", value.getNameOf());
        if (value.getPayTo() != null)
            gen.writeStringField("payTo", value.getPayTo());
        if (value.getAmount() != null)
            gen.writeNumberField("amount", value.getAmount());
        if (value.getIsDebt() != null)
            gen.writeBooleanField("isDebt", value.getIsDebt());
        if (value.getIsCheckedOut() != null)
            gen.writeBooleanField("isCheckedOut", value.getIsCheckedOut());
        if (value.getUser() != null)
            gen.writeNumberField("user", value.getUser().getId());
        if (value.getIssuedAt() != null)
            gen.writeStringField("issuedAt", value.getIssuedAt().toString());
        if (value.getValidTill() != null)
            gen.writeStringField("validTill", value.getValidTill().toString());
        if (value.getCreatedAt() != null)
            gen.writeStringField("createdAt", value.getCreatedAt().toString());
        if (value.getUpdatedAt() != null)
            gen.writeStringField("updatedAt", value.getUpdatedAt().toString());
        gen.writeEndObject();
    }


}

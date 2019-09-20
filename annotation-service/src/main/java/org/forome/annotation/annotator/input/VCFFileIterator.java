package org.forome.annotation.annotator.input;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import org.forome.annotation.controller.utils.RequestParser;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.VariantVCF;

import java.nio.file.Path;
import java.util.NoSuchElementException;

public class VCFFileIterator implements AutoCloseable {

    private final VCFFileReader vcfFileReader;
    private final CloseableIterator<VariantContext> vcfFileReaderIterator;

    private final CNVFileIterator cnvFileIterator;

    public VCFFileIterator(Path pathVcf) {
        this(pathVcf, null);
    }

    public VCFFileIterator(Path pathVcf, Path cnvFile) {
        this.vcfFileReader = new VCFFileReader(pathVcf, false);
        this.vcfFileReaderIterator = vcfFileReader.iterator();

        if (cnvFile != null) {
            cnvFileIterator = new CNVFileIterator(cnvFile);
        } else {
            cnvFileIterator = null;
        }
    }

    public Variant next() throws NoSuchElementException {
        while (true) {
            if (vcfFileReaderIterator.hasNext()) {
                VariantContext variantContext = vcfFileReaderIterator.next();
                if ("MT".equals(variantContext.getContig())) {
                    continue;//https://rm.processtech.ru/issues/1345
                }
                if ("M".equals(RequestParser.toChromosome(variantContext.getContig()))) {
                    continue;//Игнорируем митохондрии
                }
                return new VariantVCF(variantContext);
            } else if (cnvFileIterator != null && cnvFileIterator.hasNext()) {
                return cnvFileIterator.next();
            } else {
                throw new NoSuchElementException();
            }
        }
    }

    @Override
    public void close() {
        this.vcfFileReaderIterator.close();
        this.vcfFileReader.close();
    }
}

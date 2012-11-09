package org.broadinstitute.sting.gatk.walkers.bqsr;

import org.broadinstitute.sting.WalkerTest;
import org.broadinstitute.sting.utils.exceptions.UserException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author ebanks
 * @since 7/16/12
 */
public class BQSRIntegrationTest extends WalkerTest {

    private static class BQSRTest {
        final String reference;
        final String interval;
        final String bam;
        final String args;
        final String md5;

        private BQSRTest(String reference, String bam, String interval, String args, String md5) {
            this.reference = reference;
            this.bam = bam;
            this.interval = interval;
            this.args = args;
            this.md5 = md5;
        }

        public String getCommandLine() {
            return " -T BaseRecalibrator" +
                    " -R " + reference +
                    " -I " + bam +
                    " -L " + interval +
                    args +
                    " -knownSites " + (reference.equals(b36KGReference) ? b36dbSNP129 : hg18dbSNP132) +
                    " -o %s";
        }

        @Override
        public String toString() {
            return String.format("BQSR(bam='%s', args='%s')", bam, args);
        }
    }

    @DataProvider(name = "BQSRTest")
    public Object[][] createBQSRTestData() {
        String HiSeqBam = privateTestDir + "HiSeq.1mb.1RG.bam";
        String HiSeqInterval = "chr1:10,000,000-10,100,000";
        return new Object[][]{
                {new BQSRTest(hg18Reference, HiSeqBam, HiSeqInterval, "", "387b41dc2221a1a4a782958944662b25")},
                {new BQSRTest(hg18Reference, HiSeqBam, HiSeqInterval, " --no_standard_covs -cov ContextCovariate", "b5e26902e76abbd59f94f65c70d18165")},
                {new BQSRTest(hg18Reference, HiSeqBam, HiSeqInterval, " --no_standard_covs -cov CycleCovariate", "a8a9c3f83269911cb61c5fe8fb98dc4a")},
                {new BQSRTest(hg18Reference, HiSeqBam, HiSeqInterval, " --indels_context_size 4", "f43a0473101c63ae93444c300d843e81")},
                {new BQSRTest(hg18Reference, HiSeqBam, HiSeqInterval, " --low_quality_tail 5", "9e05e63339d4716584bfc717cab6bd0f")},
                {new BQSRTest(hg18Reference, HiSeqBam, HiSeqInterval, " --quantizing_levels 6", "1cf9b9c9c64617dc0f3d2f203f918dbe")},
                {new BQSRTest(hg18Reference, HiSeqBam, HiSeqInterval, " --mismatches_context_size 4", "aa1949a77bc3066fee551a217c970c0d")},
                {new BQSRTest(b36KGReference, validationDataLocation + "NA12892.SLX.SRP000031.2009_06.selected.1Mb.1RG.bam", "1:10,000,000-10,200,000", "", "f70d8b5358bc2f76696f14b7a807ede0")},
                {new BQSRTest(b36KGReference, validationDataLocation + "NA19240.chr1.BFAST.SOLID.bam", "1:10,000,000-10,200,000", "", "4c0f63e06830681560a1e9f9aad9fe98")},
                {new BQSRTest(b36KGReference, validationDataLocation + "NA12873.454.SRP000031.2009_06.chr1.10_20mb.1RG.bam", "1:10,000,000-10,200,000", "", "be2812cd3dae3c326cf35ae3f1c8ad9e")},
                {new BQSRTest(b36KGReference, validationDataLocation + "originalQuals.1kg.chr1.1-1K.1RG.bam", "1:1-1,000", " -OQ", "03c29a0c1d21f72b12daf51cec111599")},
                {new BQSRTest(b36KGReference, validationDataLocation + "NA19240.chr1.BFAST.SOLID.bam", "1:10,000,000-20,000,000", " --solid_recal_mode REMOVE_REF_BIAS", "7080b2cad02ec6e67ebc766b2dccebf8")},
                {new BQSRTest(b36KGReference, privateTestDir + "NA19240.chr1.BFAST.SOLID.hasCSNoCall.bam", "1:50,000-80,000", " --solid_nocall_strategy LEAVE_READ_UNRECALIBRATED", "30e76055c16843b6e33e5b9bd8ced57c")},
                {new BQSRTest(b36KGReference, validationDataLocation + "NA12892.SLX.SRP000031.2009_06.selected.1Mb.1RG.bam", "1:10,000,000-10,200,000", " -knownSites:anyNameABCD,VCF " + privateTestDir + "vcfexample3.vcf", "f70d8b5358bc2f76696f14b7a807ede0")},
                {new BQSRTest(b36KGReference, validationDataLocation + "NA12892.SLX.SRP000031.2009_06.selected.1Mb.1RG.bam", "1:10,000,000-10,200,000", " -knownSites:bed " + validationDataLocation + "bqsrKnownTest.bed", "5e657fd6a44dcdc7674b6e5a2de5dc83")},
        };
    }

    @Test(dataProvider = "BQSRTest")
    public void testBQSR(BQSRTest params) {
        WalkerTestSpec spec = new WalkerTestSpec(
                params.getCommandLine(),
                Arrays.asList(params.md5));
        executeTest("testBQSR-"+params.args, spec).getFirst();
    }

    @Test
    public void testBQSRFailWithoutDBSNP() {
        WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                " -T BaseRecalibrator" +
                        " -R " + b36KGReference +
                        " -I " + validationDataLocation + "NA12892.SLX.SRP000031.2009_06.selected.bam" +
                        " -L 1:10,000,000-10,200,000" +
                        " -o %s",
                1, // just one output file
                UserException.CommandLineException.class);
        executeTest("testBQSRFailWithoutDBSNP", spec);
    }

    @Test
    public void testBQSRFailWithSolidNoCall() {
        WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                " -T BaseRecalibrator" +
                        " -R " + b36KGReference +
                        " -I " + privateTestDir + "NA19240.chr1.BFAST.SOLID.hasCSNoCall.bam" +
                        " -L 1:50,000-80,000" +
                        " -o %s",
                1, // just one output file
                UserException.class);
        executeTest("testBQSRFailWithSolidNoCall", spec);
    }

    private static class PRTest {
        final String args;
        final String md5;

        private PRTest(String args, String md5) {
            this.args = args;
            this.md5 = md5;
        }

        @Override
        public String toString() {
            return String.format("PrintReads(args='%s')", args);
        }
    }

    @DataProvider(name = "PRTest")
    public Object[][] createPRTestData() {
        List<Object[]> tests = new ArrayList<Object[]>();

        tests.add(new Object[]{1, new PRTest(" -qq -1", "5226c06237b213b9e9b25a32ed92d09a")});
        tests.add(new Object[]{1, new PRTest(" -qq 6", "b592a5c62b952a012e18adb898ea9c33")});
        tests.add(new Object[]{1, new PRTest(" -DIQ", "8977bea0c57b808e65e9505eb648cdf7")});

        for ( final int nct : Arrays.asList(1, 2, 4) ) {
            tests.add(new Object[]{nct, new PRTest("", "ab2f209ab98ad3432e208cbd524a4c4a")});
        }

        return tests.toArray(new Object[][]{});
    }

    @Test(dataProvider = "PRTest")
    public void testPR(final int nct, PRTest params) {
        WalkerTestSpec spec = new WalkerTestSpec(
                "-T PrintReads" +
                        " -R " + hg18Reference +
                        " -I " + privateTestDir + "HiSeq.1mb.1RG.bam" +
                        " -nct " + nct +
                        " -BQSR " + privateTestDir + "HiSeq.20mb.1RG.table" +
                        params.args +
                        " -o %s",
                Arrays.asList(params.md5));
        executeTest("testPrintReads-"+params.args, spec).getFirst();
    }
}
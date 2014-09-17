package org.openmrs.module.rwandareports.reporting;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.InverseCohortDefinition;
import org.openmrs.module.reporting.common.SortCriteria;
import org.openmrs.module.reporting.common.SortCriteria.SortDirection;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rwandareports.util.Cohorts;
import org.openmrs.module.rwandareports.util.GlobalPropertiesManagement;
import org.openmrs.module.rwandareports.util.RowPerPatientColumns;

public class SetupOncologyInpatientClinicMissedVisit {
	
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();
	
	//properties retrieved from global variables
	private Program oncologyProgram;

	private ProgramWorkflow diagnosis;
	
	/*private Concept scheduledVisit;
	
	private Concept biopsyResultVisit;
	
	private Concept specialVisit;
	*/
	private Concept telephone;
	
	private Concept telephone2;
	
	private Concept ChemotherapyInpatientWardVisit;
	
	private Concept confirmedDiagnosis;
	
	private Form OncologyScheduleAppointmentForm;
	    
	private Form outpatientClinicVisitsForm;
	
	private Form BSAForm;
	    
	   
		private List<Concept> visitDates=new ArrayList<Concept>();
		
		private List<Form> visitForms=new ArrayList<Form>();
		
		private List<Form> preventedForms=new ArrayList<Form>();
		
		
		private List<String> onOrAfterOnOrBefore=new ArrayList<String>();
		
	
	
	public void setup() throws Exception {
		
		setupProperties();
		
		ReportDefinition rd = createReportDefinition();
		
		ReportDesign design = Helper.createRowPerPatientXlsOverviewReportDesign(rd, "OncologyInpatientClinicMissedVisit.xls",
		    "OncologyInpatientClinicMissedVisit.xls_", null);
		
		Properties props = new Properties();
		props.put("repeatingSections", "sheet:1,row:7,dataset:dataset");
		props.put("sortWeight","5000");
		design.setProperties(props);
		
		Helper.saveReportDesign(design);
	}
	
	public void delete() {
		ReportService rs = Context.getService(ReportService.class);
		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("OncologyInpatientClinicMissedVisit.xls_".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}
		Helper.purgeReportDefinition("ONC-Oncology Missed Visit Patient List - Inpatient Ward");
	}
	
	private ReportDefinition createReportDefinition() {
		
		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName("ONC-Oncology Missed Visit Patient List - Inpatient Ward");
				
		reportDefinition.addParameter(new Parameter("endDate", "Date", Date.class));	
		reportDefinition.setBaseCohortDefinition(Cohorts.createInProgramParameterizableByDate("Oncology", oncologyProgram), ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		createDataSetDefinition(reportDefinition);
		
		Helper.saveReportDefinition(reportDefinition);
		
		return reportDefinition;
	}
	
	private void createDataSetDefinition(ReportDefinition reportDefinition) {
		// Create new dataset definition 
		RowPerPatientDataSetDefinition dataSetDefinition = new RowPerPatientDataSetDefinition();
		dataSetDefinition.setName("Inpatient Missed List");
		/*RowPerPatientDataSetDefinition dataSetDefinition2 = new RowPerPatientDataSetDefinition();
		dataSetDefinition2.setName("Outpatient Biopsy Result List");
		RowPerPatientDataSetDefinition dataSetDefinition3 = new RowPerPatientDataSetDefinition();
		dataSetDefinition3.setName("Outpatient Special List");
		RowPerPatientDataSetDefinition dataSetDefinition4 = new RowPerPatientDataSetDefinition();
		dataSetDefinition3.setName("Outpatient All Lists");
		*/
		
		dataSetDefinition.addParameter(new Parameter("endDate", "Date", Date.class));
		/*dataSetDefinition2.addParameter(new Parameter("endDate", "Date", Date.class));
		dataSetDefinition3.addParameter(new Parameter("endDate", "Date", Date.class));
		dataSetDefinition4.addParameter(new Parameter("endDate", "Date", Date.class));
		*/
		SortCriteria sortCriteria = new SortCriteria();
		sortCriteria.addSortElement("nextRDVDate", SortDirection.ASC);
		dataSetDefinition.setSortCriteria(sortCriteria);
		/*dataSetDefinition2.setSortCriteria(sortCriteria);
		dataSetDefinition3.setSortCriteria(sortCriteria);
		dataSetDefinition4.setSortCriteria(sortCriteria);
		*/
		//Add filters
		dataSetDefinition.addFilter(Cohorts.createPatientsLateForVisit(ChemotherapyInpatientWardVisit, visitForms), ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		
		
		// who do not have a recorded visit (outpatient, BSA) since the scheduled visit date
		
		dataSetDefinition.addFilter(new InverseCohortDefinition(Cohorts.createEncounterBasedOnForms("patientsWithFlowFormAndBSA",onOrAfterOnOrBefore,preventedForms)), ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-6d},onOrBefore=${endDate}"));

		
		
		/*dataSetDefinition2.addFilter(Cohorts.createPatientsLateForVisit(biopsyResultVisit, visitForms), ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition3.addFilter(Cohorts.createPatientsLateForVisit(specialVisit, visitForms), ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition4.addFilter(Cohorts.createPatientsLateForVisit(visitDates, visitForms), ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		*/
		//Add Columns
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("nextRDV", ChemotherapyInpatientWardVisit, "dd/MMM/yyyy"), new HashMap<String, Object>());
		/*dataSetDefinition2.addColumn(RowPerPatientColumns.getMostRecent("nextRDV", biopsyResultVisit, "dd/MMM/yyyy"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getMostRecent("nextRDV", specialVisit, "dd/MMM/yyyy"), new HashMap<String, Object>());
		*/
		
		/*dataSetDefinition4.addColumn(RowPerPatientColumns.getMostRecent("nextRDVscheduledVisit", scheduledVisit, "dd/MMM/yyyy"), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getMostRecent("nextRDVbiopsyResultVisit", biopsyResultVisit, "dd/MMM/yyyy"), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getMostRecent("nextRDVspecialVisit", specialVisit, "dd/MMM/yyyy"), new HashMap<String, Object>());
		*/
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("nextRDVDate", ChemotherapyInpatientWardVisit, "yyyy/MM/dd"), new HashMap<String, Object>());
		/*dataSetDefinition2.addColumn(RowPerPatientColumns.getMostRecent("nextRDVDate", biopsyResultVisit, "yyyy/MM/dd"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getMostRecent("nextRDVDate", specialVisit, "yyyy/MM/dd"), new HashMap<String, Object>());
		*/
		dataSetDefinition.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"), new HashMap<String, Object>());
		/*dataSetDefinition2.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"), new HashMap<String, Object>());
		*/
		dataSetDefinition.addColumn(RowPerPatientColumns.getMiddleNameColumn("middleName"), new HashMap<String, Object>());
		/*dataSetDefinition2.addColumn(RowPerPatientColumns.getMiddleNameColumn("middleName"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getMiddleNameColumn("middleName"), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getMiddleNameColumn("middleName"), new HashMap<String, Object>());
		*/
		dataSetDefinition.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"), new HashMap<String, Object>());
		/*dataSetDefinition2.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"), new HashMap<String, Object>());
		*/
		dataSetDefinition.addColumn(RowPerPatientColumns.getIMBId("id"), new HashMap<String, Object>());
		/*dataSetDefinition2.addColumn(RowPerPatientColumns.getIMBId("id"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getIMBId("id"), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getIMBId("id"), new HashMap<String, Object>());
		*/ 
		/*dataSetDefinition2.addColumn(RowPerPatientColumns.getStateOfPatient("diagnosis", oncologyProgram, diagnosis, null), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getStateOfPatient("diagnosis", oncologyProgram, diagnosis, null), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getStateOfPatient("diagnosis", oncologyProgram, diagnosis, null), new HashMap<String, Object>());
		*/
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getStateOfPatient("diagnosis", oncologyProgram, diagnosis, null), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("diagnosisNew", confirmedDiagnosis, null), new HashMap<String, Object>());
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("telephone", telephone, null), new HashMap<String, Object>());
		
		
		/*dataSetDefinition2.addColumn(RowPerPatientColumns.getMostRecent("telephone", telephone, null), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getMostRecent("telephone", telephone, null), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getMostRecent("telephone", telephone, null), new HashMap<String, Object>());
		*/
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("telephone2", telephone2, null), new HashMap<String, Object>());
		/*dataSetDefinition2.addColumn(RowPerPatientColumns.getMostRecent("telephone2", telephone2, null), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getMostRecent("telephone2", telephone2, null), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getMostRecent("telephone2", telephone2, null), new HashMap<String, Object>());
		*/
		dataSetDefinition.addColumn(RowPerPatientColumns.getPatientAddress("address", true, true, true, true),
		    new HashMap<String, Object>());
		/*dataSetDefinition2.addColumn(RowPerPatientColumns.getPatientAddress("address", true, true, true, true),
		    new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getPatientAddress("address", true, true, true, true),
		    new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getPatientAddress("address", true, true, true, true),
			    new HashMap<String, Object>());
		*/	
		dataSetDefinition.addColumn(RowPerPatientColumns.getAccompRelationship("accompagnateur"), new HashMap<String, Object>());
		/*dataSetDefinition2.addColumn(RowPerPatientColumns.getAccompRelationship("accompagnateur"), new HashMap<String, Object>());
		dataSetDefinition3.addColumn(RowPerPatientColumns.getAccompRelationship("accompagnateur"), new HashMap<String, Object>());
		dataSetDefinition4.addColumn(RowPerPatientColumns.getAccompRelationship("accompagnateur"), new HashMap<String, Object>());
		*/
		Map<String, Object> mappings = new HashMap<String, Object>();
		mappings.put("endDate", "${endDate}");
		
		reportDefinition.addDataSetDefinition("dataset", dataSetDefinition, mappings);
		/*reportDefinition.addDataSetDefinition("dataset2", dataSetDefinition2, mappings);
		reportDefinition.addDataSetDefinition("dataset3", dataSetDefinition3, mappings);
		reportDefinition.addDataSetDefinition("datasetAll", dataSetDefinition4, mappings);
	*/}
	
	private void setupProperties() {
		
		oncologyProgram = gp.getProgram(GlobalPropertiesManagement.ONCOLOGY_PROGRAM);
		
		diagnosis = gp.getProgramWorkflow(GlobalPropertiesManagement.DIAGNOSIS_WORKFLOW, GlobalPropertiesManagement.ONCOLOGY_PROGRAM);
		
		/*scheduledVisit = gp.getConcept(GlobalPropertiesManagement.ONCOLOGY_SCHEDULED_OUTPATIENT_VISIT);
		
		biopsyResultVisit = gp.getConcept(GlobalPropertiesManagement.ONCOLOGY_PATHOLOGY_RESULT_VISIT);
		
		specialVisit = gp.getConcept(GlobalPropertiesManagement.ONCOLOGY_SPECIAL_VISIT);
		
		*/telephone = gp.getConcept(GlobalPropertiesManagement.TELEPHONE_NUMBER_CONCEPT);
		
		telephone2 = gp.getConcept(GlobalPropertiesManagement.SECONDARY_TELEPHONE_NUMBER_CONCEPT);
		
		OncologyScheduleAppointmentForm=gp.getForm(GlobalPropertiesManagement.ONCOLOGY_SCHEDULE_APPOINTMENT_FORM);
		
		outpatientClinicVisitsForm=gp.getForm(GlobalPropertiesManagement.OUTPATIENT_CLINIC_VISITS_FORM);		
		

		BSAForm=gp.getForm(GlobalPropertiesManagement.BSA_VISITS_FORM);
		
		preventedForms.add(outpatientClinicVisitsForm);
		preventedForms.add(BSAForm);
		
		
		ChemotherapyInpatientWardVisit=gp.getConcept(GlobalPropertiesManagement.CHEMOTHERAPY_INPATIENT_WARD_VISIT_DATE);
		
		
		
		visitForms.add(OncologyScheduleAppointmentForm);
		/*visitForms.add(outpatientClinicVisitsForm);*/
		
		visitDates.add(ChemotherapyInpatientWardVisit);
		/*visitDates.add(biopsyResultVisit);
		visitDates.add(specialVisit);
		*/
		
		onOrAfterOnOrBefore.add("onOrAfter");
		onOrAfterOnOrBefore.add("onOrBefore");
		
		confirmedDiagnosis=gp.getConcept(GlobalPropertiesManagement.CONFIRMED_DIAGNOSIS_CONCEPT);
	}
}
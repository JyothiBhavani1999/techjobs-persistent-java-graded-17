package org.launchcode.techjobs.persistent;

import jakarta.persistence.ManyToMany;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.jupiter.api.Test;
import org.launchcode.techjobs.persistent.controllers.HomeController;
import org.launchcode.techjobs.persistent.controllers.ListController;
import org.launchcode.techjobs.persistent.models.Employer;
import org.launchcode.techjobs.persistent.models.Job;
import org.launchcode.techjobs.persistent.models.Skill;
import org.launchcode.techjobs.persistent.models.data.EmployerRepository;
import org.launchcode.techjobs.persistent.models.data.JobRepository;
import org.launchcode.techjobs.persistent.models.data.SkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by LaunchCode
 */
public class TestTaskFour extends AbstractTest {

    @Test
    public void testSkillClassHasJobsField () throws ClassNotFoundException {
        Class skillClass = getClassByName("models.Skill");
        Field jobsField = null;

        try {
            jobsField = skillClass.getDeclaredField("jobs");
        } catch (NoSuchFieldException e) {
            fail("Skill class does not have a jobs field");
        }
    }

    @Test
    public void testSkillJobsFieldHasCorrectType () throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class skillClass = getClassByName("models.Skill");
        Method getJobsMethod = skillClass.getMethod("getJobs");
        Skill skill = new Skill();
        Object jobsObj = getJobsMethod.invoke(skill);
        assertTrue(jobsObj instanceof List);
    }

    @Test
    public void testSkillJobsFieldHasCorrectAnnotation () throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class skillClass = getClassByName("models.Skill");
        Field jobsField = skillClass.getDeclaredField("jobs");
        Annotation annotation = jobsField.getDeclaredAnnotation(ManyToMany.class);
        assertNotNull(annotation);
        Method mappedByMethod = annotation.getClass().getMethod("mappedBy");
        assertEquals("skills", mappedByMethod.invoke(annotation));
    }


    @Test
    public void testJobSkillsHasCorrectTypeAndAnnotation () throws ClassNotFoundException, NoSuchFieldException {
        Class jobClass = getClassByName("models.Job");
        Field skillsField = jobClass.getDeclaredField("skills");
        Type skillsFieldType = skillsField.getType();
        assertEquals(List.class, skillsFieldType, "Job.skills should be of type List<Skills>");
        assertNotNull(skillsField.getAnnotation(ManyToMany.class), "Job.skills is missing the correct mapping annotation");
    }


    @Test
    public void testJobSkillsRefactoring () throws ClassNotFoundException, NoSuchMethodException {
        Class jobClass = getClassByName("models.Job");
        try {
            Constructor nonDefaultConstructor = jobClass.getConstructor(Employer.class, List.class);
        } catch (NoSuchMethodException e) {
            fail("The non-default constructor has not been refactored to handle the new skills field type");
        }

        Method getSkillsMethod = jobClass.getMethod("getSkills");
        getSkillsMethod.getReturnType().isInstance(List.class);

        try {
            jobClass.getMethod("setSkills", List.class);
        } catch (NoSuchMethodException e) {
            fail("Job.setSkills has not been refactoring to handle the new skills field type");
        }
    }


    @Test
    public void testHomeControllerHasSkillRepository () throws ClassNotFoundException {
        Class homeControllerClass = getClassByName("controllers.HomeController");
        Field skillRepositoryField = null;
        try {
            skillRepositoryField = homeControllerClass.getDeclaredField("skillRepository");
        } catch (NoSuchFieldException e) {
            fail("HomeController should have a skillRepository field");
        }

        assertEquals(SkillRepository.class, skillRepositoryField.getType(), "skillRepository is of incorrect type");
        assertNotNull(skillRepositoryField.getAnnotation(Autowired.class), "skillRepository must be @Autowired");
    }

    @Test
    public void testProcessAddJobFormHandlesSkillsProperly (
            @Mocked SkillRepository skillRepository,
            @Mocked EmployerRepository employerRepository,
            @Mocked JobRepository jobRepository,
            @Mocked Job job,
            @Mocked Errors errors)
            throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InvocationTargetException {
        Class homeControllerClass = getClassByName("controllers.HomeController");
        Method processAddJobFormMethod = homeControllerClass.getMethod("processAddJobForm", Job.class, Errors.class, Model.class, int.class, List.class);

        new Expectations() {{
            skillRepository.findAllById((Iterable<Integer>) any);
            job.setSkills((List<Skill>) any);
        }};

        Model model = new ExtendedModelMap();
        HomeController homeController = new HomeController();

        Field skillRepositoryField = homeControllerClass.getDeclaredField("skillRepository");
        skillRepositoryField.setAccessible(true);
        skillRepositoryField.set(homeController, skillRepository);

        Field employerRepositoryField = homeControllerClass.getDeclaredField("employerRepository");
        employerRepositoryField.setAccessible(true);
        employerRepositoryField.set(homeController, employerRepository);

        Field jobRepositoryField = homeControllerClass.getDeclaredField("jobRepository");
        jobRepositoryField.setAccessible(true);
        jobRepositoryField.set(homeController, jobRepository);

        processAddJobFormMethod.invoke(homeController, job, errors, model, 0, new ArrayList<Skill>());
    }


    @Test
    public void testListControllerHasAutowiredRepositories () throws ClassNotFoundException {
        Class listControllerClass = getClassByName("controllers.ListController");
        Field employerRepositoryField = null;
        Field skillRepositoryField = null;

        try {
            employerRepositoryField = listControllerClass.getDeclaredField("employerRepository");
        } catch (NoSuchFieldException e) {
            fail("ListController must have an employerRepository field");
        }

        assertEquals(EmployerRepository.class, employerRepositoryField.getType());
        assertNotNull(employerRepositoryField.getAnnotation(Autowired.class));

        try {
            skillRepositoryField = listControllerClass.getDeclaredField("skillRepository");
        } catch (NoSuchFieldException e) {
            fail("ListController must have a skillRepository field");
        }

        assertEquals(SkillRepository.class, skillRepositoryField.getType());
        assertNotNull(skillRepositoryField.getAnnotation(Autowired.class));
    }

    @Test
    public void testSqlQuery () throws IOException {
        String queryFileContents = getFileContents("queries.sql");

        Pattern queryPattern = Pattern.compile("SELECT\\s+\\*\\s+FROM\\s+skill" +
                "\\s*(LEFT|INNER)?\\s+JOIN\\s+job_skills\\s+ON\\s+(skill.id\\s+=\\s+job_skills.skills_id|job_skills.skills_id\\s+=\\s+skill.id)" +
                "(\\s*WHERE\\s+job_skills.jobs_id\\s+IS\\s+NOT\\s+NULL)?" +
                "\\s*ORDER\\s+BY\\s+name\\s+ASC;", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher queryMatcher = queryPattern.matcher(queryFileContents);
        boolean queryFound = queryMatcher.find();
        assertTrue(queryFound, "Task 4 SQL query is incorrect. Test your query against your database to find the error.");
    }

}

--Part 1
id , int , pri key
name , varchar
employer , varchar
skills , varchar


SELECT name FROM employer WHERE location = "St. Louis City";Part 2

--

--Part 3

drop table job;


--Part 4
SELECT * FROM skill
LEFT JOIN job_skills ON skill.id = job_skills.skills_id
WHERE job_skills.jobs_id IS NOT NULL
ORDER BY name ASC;
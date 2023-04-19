echo
   set dataset=RE
   set rcs=(0.0 0.05 0.1 0.15 0.2 0.25)
   set rvs=(0.0 0.05 0.1 0.15 0.2 0.25)

   cd "F:\Google Drive\Aulas\Doutorado\UFSC\Pesquisa DOC\Prototipo v2\Ext TimeSeq\MAT-SG_TimeSeq"
   mkdir "datasets\%dataset%\output"
    set file=Running_Example_v5
    set fileAll=Running_Example_v5_complete
    for %%v in %rvs% do for %%x in %rcs% do (
           rem call echo %%u
           rem call echo %%file%%
           call java -jar "dist\MAT-SG_TimeSeq.jar" %%file%% "R" %%x %%v %%fileAll%%
     )
   rem cd "F:\Google Drive\Aulas\Doutorado\UFSC\Pesquisa DOC\Prototipo v2\Ext TimeSeq\MAT-SG_TimeSeq"
   cd "F:\Google Drive\Aulas\Doutorado\UFSC\Pesquisa DOC\Prototipo v2\Ext TimeSeq\MAT-SG_TimeSeq\datasets\%dataset%"
   call ren "output" "output - validation - "%%file%%
   )
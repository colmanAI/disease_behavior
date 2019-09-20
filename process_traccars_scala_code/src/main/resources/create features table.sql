USE [traccar]
GO

/****** Object:  Table [dbo].[features]    Script Date: 15/12/2017 15:07:47 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[features](
	[deviceid] [int] NOT NULL,
	[date] [date] NOT NULL,
	[trips] [int] NOT NULL,
	[totalTripsDistance] [float] NOT NULL,
	[averageTripDistance] [float] NOT NULL,
	[maxTripDiameter] [float] NOT NULL,
	[totalTripsDuration] [float] NOT NULL,
	[averageTripDuration] [float] NOT NULL,
	[maxTripDuration] [float] NOT NULL,
	[hasSleptHome] [bit] NOT NULL
) ON [PRIMARY]

GO

EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'in meters' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'features', @level2type=N'COLUMN',@level2name=N'totalTripsDistance'
GO

EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'in meters' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'features', @level2type=N'COLUMN',@level2name=N'averageTripDistance'
GO

EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'in meters' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'features', @level2type=N'COLUMN',@level2name=N'maxTripDiameter'
GO

EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'in minutes' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'features', @level2type=N'COLUMN',@level2name=N'totalTripsDuration'
GO

EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'in minutes' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'features', @level2type=N'COLUMN',@level2name=N'averageTripDuration'
GO

EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'in minutes' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'features', @level2type=N'COLUMN',@level2name=N'maxTripDuration'
GO
